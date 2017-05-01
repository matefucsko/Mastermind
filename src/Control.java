
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Control {

	private int colourNum;
	private boolean colourRepeat;
	private GameState gs;
	private GUI gui;
	private Network net = null;
	private int[] problem;
	private boolean SinglePlayer;
	private int OtherPlayerScore;
	public boolean MyScoreSent;
	private long StartTime;
	private Command cmd;


	public class GameControlThread implements Runnable {

		public void run() {
			System.out.println("Starting Control Thread...");
			try {
				while (true) {
					if (cmd.dirtyBit == true) {
						switch (cmd.lastPressedButton) {
						case "Settings_OK":
							colourNum = cmd.colourNum;
							gs.numColors = cmd.colourNum;
							colourRepeat = cmd.colourRepeat;
							gs.colourRepeat = cmd.colourRepeat;
							gs.LastChanged = "Settings";
							gui.onNewGameState(gs);
							System.out.printf("Colours: %d\tRepeatable:%b\n", colourNum, colourRepeat);
							break;
						case "Start1P":
							gs.tryMax = colourNum * 2 - 2;
							System.out.printf("Start1P\n");
							Start1Player();
							break;
						case "Start2P":
							if (cmd.server) {
								gs.tryMax = colourNum * 2 - 2;
								System.out.printf("Start 2P Game - Server \n");
								Start2Player();
							} else {
								System.out.printf("Start 2P Game - Server \n");
								Start2Player(cmd.IP);
							}
							break;
						case "Dot":
							System.out.printf("Dot:\t");
							if(cmd.line==gs.ActualRow){
								if(gs.Dots[cmd.column][cmd.line] != cmd.pickedColour)
									gs.Dots[cmd.column][cmd.line] = cmd.pickedColour;
								else gs.Dots[cmd.column][cmd.line] = null;
								gs.LastChanged="Board";
								gui.onNewGameState(gs);
							}
							System.out.printf("Line=%d\tColumn=%d\n", cmd.line, cmd.column);

							break;
						case "ColorChooser":
							System.out.printf("colour= %s\n", cmd.pickedColour);
							gs.pickedColour = cmd.pickedColour;
							break;
						case "LineAccepter":
							System.out.printf("LineAccepter\n");
							UserClick_LineAccepter();
							break;
						case "Highscores":
							System.out.printf("Highscores\n");
							gui.ListHighScores(HighScore2HTML());
							break;	
						case "Exit":
							System.out.printf("Exit Game\n");
							CleanUpGame();
							System.out.printf("Exit Game\n");
							break;

						default:
							System.out.printf("WTF? There is no such button.\n");
							break;
						}
						cmd.dirtyBit = false;
					}
					// TODO
					else
						//System.out.printf(".");
					Thread.sleep(200);
				}
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
				System.err.println("");
			} finally {
			}
		}
	}

	/***********************
	 * Interface functions *
	 **********************/

	public void onCommand(Command C) {
		cmd = C;
		cmd.dirtyBit = true;
	}
	
	/********************
	 * Game functions   *
	 *******************/

	public void Start1Player() {
		SinglePlayer = true;
		problem = generateProblem(colourNum, colourRepeat);
		gui.StartGame(gs.tryMax, colourNum);
		gs.ActualRow = 0;
		// TESZTELESRE
		System.out.printf("A feladat:\t");
		for (int i = 0; i < 4; i++)
			System.out.printf("%s\t", GUI.COLOR.values()[problem[i]]);
		System.out.println();
		gs.problem=problem;
		StartTime = System.currentTimeMillis();
	}

	public void Start2Player(String IP) {
		SinglePlayer = false;
		MyScoreSent = false;
		OtherPlayerScore = 42;
		gs.ActualRow = 0;
		StartClient(IP);
	}

	public void Start2Player() {
		SinglePlayer = false;
		MyScoreSent = false;
		gs.ActualRow = 0;
		OtherPlayerScore = 42;
		StartServer();
	}

	public void UserClick_LineAccepter() throws IOException {
		int[] dots = new int[4];
		for (int i = 0; i < 4; i++) {
			if (gs.Dots[i][gs.ActualRow] == null)
				dots[i] = -1;
			else
				dots[i] = gs.Dots[i][gs.ActualRow].ordinal();
		}
		boolean win = EvaluateTry(colourNum, problem, dots);
		gs.ActualRow++;
		gui.onNewGameState(gs);
		GUI.COLOR[] colors = new GUI.COLOR[4];
		for (int i = 0; i < 4; i++)
			colors[i] = GUI.COLOR.values()[problem[i]];
		if (SinglePlayer) {
			if (win) {
				gui.RevealProblem(colors);
				long estimatedTime = System.currentTimeMillis() - StartTime;
				CompareToHighScores(estimatedTime);
				CleanUpGame();
			}
			if (gs.ActualRow == gs.tryMax) {
				gui.RevealProblem(colors);
				long estimatedTime = System.currentTimeMillis() - StartTime;
				int min = (int) estimatedTime / 60000;
				int sec = (int) (estimatedTime / 1000) % 60;
				gui.MessageAfterGame("Winner! \n Number of tries: " + gs.ActualRow + "\n Time: " + min + ":" + sec + "\n");
				CleanUpGame();
			}
		} else {
			if (win) {
				SendScore();
				gui.RevealProblem(colors);
				long estimatedTime = System.currentTimeMillis() - StartTime;
				int min = (int) estimatedTime / 60000;
				int sec = (int) (estimatedTime / 1000) % 60;
				if (OtherPlayerScore != 42) { // Megvan a masik jatekos
												// eredmenye
					if (gs.ActualRow < OtherPlayerScore)
						gui.MessageAfterGame(
								"Winner! \n Number of tries: " + gs.ActualRow + "\n Time: " + min + ":" + sec + "\n");
					else
						gui.MessageAfterGame(
								"You lost! \n Number of tries: " + gs.ActualRow + "\n Time: " + min + ":" + sec + "\n");
				}
				// WAIT FOR OTHER PLAYER
				else {
					gui.MessageAfterGame("Waiting for the other player...");
					for (int i = 30; i > 0 && OtherPlayerScore == 42; i--) {

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (gs.ActualRow <= OtherPlayerScore)
						gui.MessageAfterGame(
								"Winner! \n Number of tries: " + gs.ActualRow + "\n Time: " + min + ":" + sec + "\n");
					else
						gui.MessageAfterGame(
								"You lost! \n Number of tries: " + gs.ActualRow + "\n Time: " + min + ":" + sec + "\n");
				}
				CleanUpGame();
				net.disconnect();
			} else if (gs.ActualRow == gs.tryMax) {
				SendScore();
				gui.RevealProblem(colors);
				long estimatedTime = System.currentTimeMillis() - StartTime;
				int min = (int) estimatedTime / 60000;
				int sec = (int) (estimatedTime / 1000) % 60;
				gui.MessageAfterGame(
						"You lost! \n Number of tries: " + gs.ActualRow + "\n Time: " + min + ":" + sec + "\n");
				CleanUpGame();
			}
		}
	}

	public int[] generateProblem(int colourNum, boolean colourRepeat) {
		int[] dots = new int[4];
		for (int i = 0; i < 4; i++) {
			double d = colourNum * Math.random();
			dots[i] = (int) Math.round(d - 0.5);
			if (colourRepeat == false) {
				for (int j = 0; j < i; j++) {
					if (dots[j] == dots[i]) {
						i--; // Ujra randomgenerálom az i. tagot
						break;
					}
				}
			}
			// System.out.printf("%d : %d\n",i,dots[i]);
		}
		return dots;
	}

	public boolean EvaluateTry(int colourNum, int[] problem, int[] proba) {
		int black = 0;
		int white = 0;
		boolean win;
		boolean[] evaluated = new boolean[4]; // a tippekbõl melyik van már
												// értékelve
		for (int i = 0; i < 4; i++)
			evaluated[i] = false;
		boolean[] used = new boolean[4]; // a feladat adott pöcke már beszámit
											// az értékelésbe
		for (int i = 0; i < 4; i++)
			used[i] = false;
		// black:
		for (int i = 0; i < 4; i++) {
			if (problem[i] == proba[i]) {
				black++;
				evaluated[i] = true;
				used[i] = true;
			}
		}
		// white:
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (evaluated[i] == false) {
					if (proba[i] == problem[j] && used[j] == false) {
						white++;
						evaluated[i] = true;
						used[j] = true;
					}
				}
			}
		}
		if (black == 4)
			win = true;
		else
			win = false;
		// sticks:
		for (int i = 0; i < 2; i++) {
			if (black > 0) {
				gs.Sticks[i][gs.ActualRow * 2]=GUI.COLOR.values()[7];
				black--;
			} else if (white > 0) {
				gs.Sticks[i][gs.ActualRow * 2]=GUI.COLOR.values()[8];
				white--;
			}
		}
		for (int i = 0; i < 2; i++) {
			if (black > 0) {
				gs.Sticks[i][gs.ActualRow * 2 + 1]=GUI.COLOR.values()[7];
				black--;
			} else if (white > 0) {
				gs.Sticks[i][gs.ActualRow * 2 + 1]=GUI.COLOR.values()[8];
				white--;
			}
		}
		return win;
	}
	private String HighScore2HTML() throws IOException{
		String content;
		if(colourRepeat) content = new String(Files.readAllBytes(Paths.get("highscore"+colourNum+"r.txt")));
		else content = new String(Files.readAllBytes(Paths.get("highscore"+colourNum+".txt")));
		String[] lines = content.split("#");
		String[] names = new String[11];
		String[] scores = new String[11];
		String[] times = new String[11];
		int[] min = new int[11];
		int[] sec = new int[11];
		for (int i = 0; i < 10; i++) {
			names[i] = lines[i].split("@")[0];
			scores[i] = lines[i].split("@")[1];
			times[i] = lines[i].split("@")[2];
			min[i]= (int) (Long.parseLong(times[i]) / 60000);
			sec[i]= (int) (Long.parseLong(times[i]) / 1000) % 60;
		}
		String highscore="<html>"
				+	"<table>"
				+		 "<tr>"
				 + "			<th>Helyezés</th>"
				 + "			<th>Név</b></th>"
				 + "			<th>Próbálkozások</b></th>"				 
				 + "			<th>Idõ</b></th>"
				 + "		</tr>";
		for (int i=0; i < 10; i++){
			highscore=highscore
				 + "		<tr>"
				 + "			<th style=\"font-weight: normal;\">"+(i+1)+".</th>"
				 + "			<th style=\"font-weight: normal;\">"+names[i]+"</th>"
				 + "			<th style=\"font-weight: normal;\">"+scores[i]+"</th>"
				 + "			<th style=\"font-weight: normal;\">"+min[i]+":"+sec[i]+"</th>"
				 + "		</tr>";
		}
		highscore=highscore+"</table></html>";
		return highscore;
	}
	private void CompareToHighScores(long myTime) throws IOException {
		//makeHighScoreFiles();
		String content;
		String filename;
		if(colourRepeat) filename ="highscore"+colourNum+"r.txt";
		else filename ="highscore"+colourNum+".txt";
		content = new String(Files.readAllBytes(Paths.get(filename)));
		String[] lines = content.split("#");
		String[] names = new String[11];
		String[] scores = new String[11];
		String[] times = new String[11];
		for (int i = 0; i < 10; i++) {
			names[i] = lines[i].split("@")[0];
			scores[i] = lines[i].split("@")[1];
			times[i] = lines[i].split("@")[2];
		}
		int place = 10;
		for (int i = 9; i >= 0; i--) {
			if (gs.ActualRow < Integer.parseInt(scores[i])) { // Kevesebb
															// probalkozas
				place = i;
			} else if (gs.ActualRow == Integer.parseInt(scores[i]) && myTime < Long.parseLong(times[i])) // Ugyanannyi
																										// probalkozas,
																										// rovidebb
																										// ido
				place = i;
		}
		if (place < 10) { // Felkerult a toplistara
			int min = (int) myTime / 60000;
			int sec = (int) (myTime / 1000) % 60;
			String Name = gui.getName("<html><center>Congratulations!</center></html>"
					+ "\n You made it to the top 10!\n"
					+ "Number of tries:\t"	+ (gs.ActualRow)
					+ "\nTime:\t" + min + ":" + sec + "\n"
					+ "Type in your name to save your result!");
			String highscores = insertScore(Name, myTime, lines, place);
			try (Writer writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
				writer.write(highscores);
			}
		}
	}

	private String insertScore(String name, long myTime, String[] lines, int place) {
		String[] newlines = new String[10];
		for (int i = 0; i < place; i++) {
			newlines[i] = lines[i]+"#";
		}
		newlines[place] = name + "@" + (gs.ActualRow) + "@" + myTime + "#";
		for (int i = place; i < 9; i++) {
			newlines[i + 1] = lines[i]+"#";
		}
		String highscore = "";
		for (int i = 0; i < 10; i++) {
			highscore = highscore + newlines[i];
		}
		return highscore;
	}

	private void makeHighScoreFiles() throws UnsupportedEncodingException, FileNotFoundException, IOException{
		String highscores="";
		for(int i=0; i<10; i++){
			highscores=highscores+"Gipsz Jakab@99@9999999#";
		}
		for(int i=4; i<11; i++){
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(
	              new FileOutputStream("highscore"+i+".txt"), "utf-8"))) {
				writer.write(highscores);
			}
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("highscore"+i+"r.txt"), "utf-8"))) {
				writer.write(highscores);
			}
		}
	}
	
	public void CleanUpGame(){
		for(int line = 0; line < gs.tryMax; line++){
			for(int col=0; col<=3; col++){
				gs.Dots[col][line]=null;
			}
		}
		for(int line = 0; line < gs.tryMax*2; line++){
			for(int col=0; col<2; col++){
				gs.Sticks[col][line]=null;
			}
		}
		gs.LastChanged="Exit";
		gui.onNewGameState(gs);
	}

	void StartServer() {
		if (net != null)
			net.disconnect();
		net = new SerialServer(this);
		net.connect("localhost");
	}

	void StartClient(String IP) {
		if (net != null)
			net.disconnect();
		net = new SerialClient(this);
		net.connect(IP);
	}

	void SendProblem() {
		if (net == null)
			return;
		int[] pack = new int[5];
		for (int i = 0; i < 4; i++)
			pack[i] = problem[i];
		pack[4] = colourNum;
		net.send(pack);
	}

	void ReceivedProblem(int[] pack) {
		for (int i = 0; i < 4; i++)
			problem[i] = pack[i];
		colourNum = pack[4];
		gs.tryMax = colourNum * 2 - 2;
		// TESZTELESRE
		System.out.printf("A feladat:\t");
		for (int i = 0; i < 4; i++)
			System.out.printf("%s\t", GUI.COLOR.values()[problem[i]]);
		System.out.println();

		gs.numColors = colourNum;
		gui.StartGame(gs.tryMax, colourNum);
		StartTime = System.currentTimeMillis();
	}

	void ClientConnected() {
		problem = generateProblem(colourNum, colourRepeat);
		SendProblem();
		// TESZTELESRE
		System.out.printf("A feladat:\t");
		for (int i = 0; i < 4; i++)
			System.out.printf("%s\t", GUI.COLOR.values()[problem[i]]);
		System.out.println();

		gui.StartGame(gs.tryMax, colourNum);
		StartTime = System.currentTimeMillis();
	}

	void ReceivedScore(int[] pack) {
		System.out.println("Recieved the other player's score. Waiting for you...");
		OtherPlayerScore = pack[0];
	}

	void SendScore() {
		System.out.println("Sending score");
		int[] pack = new int[5];// Kesobb bovitheto pl idovel
		pack[0] = gs.ActualRow;
		for (int i = 1; i < 5; i++)
			pack[i] = 0;
		net.send(pack);
		MyScoreSent = true;
	}

	void setGUI(GUI g) {
		gui = g;
	}

	void setGameState(GameState gs) {
		this.gs = gs;
	}

	Control() {
		colourNum = 6;
		colourRepeat = false;
		gs = new GameState();		
		problem = new int[4];
		cmd = new Command();
		Thread LogicThread = new Thread(new GameControlThread());
		LogicThread.start();
	}
}
