
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Control {

	public int colourNum;
	public boolean colourRepeat;
	public int tryMax;
	private GUI gui;
	private Network net = null;
	int pickedColour;
	private int[] problem;
	int ActualRow;
	private boolean SinglePlayer;
	private int OtherPlayerScore;
	private int OtherPlayerTime;
	public boolean MyScoreSent;
	private long StartTime;
	void setGUI(GUI g) {
		gui = g;
		colourNum = g.NumColors;
		colourRepeat = g.RepeatableColors;
		pickedColour = 0;
	}

	Control() {
		colourNum = 6;
		colourRepeat = false;
		tryMax = 10;
		pickedColour = 0;
		ActualRow = 0;
		problem = new int[4];
	}

	/***********************
	 * Interface functions *
	 **********************/

	public void UserClick_Dots(int column, int line) {
		if (line == ActualRow) {
			if (gui.getDots()[column][line] == null) {
				gui.AddDot(line, column, GUI.COLOR.values()[pickedColour]);
			} else {
				gui.RemoveDot(line, column);
			}
		}
		// System.out.printf("%d\t%d\t%s\t", column, line,
		// GUI.COLOR.values()[pickedColour]);

	}

	public void Start1Player(int NumColors, boolean RepeatableColors) {
		SinglePlayer = true;
		problem = generateProblem(NumColors, RepeatableColors);
		colourNum = NumColors;
		colourRepeat = RepeatableColors;
		ActualRow = 0;
		// TESZTELESRE
		System.out.printf("A feladat:\t");
		for (int i = 0; i < 4; i++)
			System.out.printf("%s\t", GUI.COLOR.values()[problem[i]]);
		System.out.println();
		StartTime=System.currentTimeMillis();
	}

	public void Start2Player(String IP) {
		SinglePlayer = false;
		MyScoreSent=false;
		OtherPlayerScore = 42;
		StartClient();
	}

	public void Start2Player(int NumColors, boolean RepeatableColors) {
		SinglePlayer = false;
		MyScoreSent=false;
		OtherPlayerScore = 42;
		StartServer();
		colourNum = NumColors;
		colourRepeat = RepeatableColors;
	}

	public void ExitGame() {
		// if(2player) GUI.OtherPlayerDisconnected();
	}

	public void UserClick_LineAccepter(int index) throws IOException {
		int[] dots = new int[4];
		for (int i = 0; i < 4; i++) {
			if (gui.getDots()[i][ActualRow] == null)
				dots[i] = -1;
			else
				dots[i] = gui.getDots()[i][ActualRow].ordinal();
		}
		boolean win = EvaluateTry(colourNum, problem, dots);
		ActualRow++;
		GUI.COLOR[] colors = new GUI.COLOR[4];
		for (int i = 0; i < 4; i++)
			colors[i] = GUI.COLOR.values()[problem[i]];
		if (SinglePlayer) {
			if (win) {
				gui.RevealProblem(colors);
				long estimatedTime = System.currentTimeMillis()  - StartTime;
				int min=(int) estimatedTime/60000;
				int sec=(int) (estimatedTime/1000)%60;
				gui.MessageAfterGame("Winner! \n Number of tries: "+ActualRow+"\n Time: "+min+":"+sec+"\n");
				// RefreshHighScore();
				gui.CleanUpGame();
			}
			if (ActualRow == 2 * colourNum) {
				gui.RevealProblem(colors);
				long estimatedTime = System.currentTimeMillis()  - StartTime;
				int min=(int) estimatedTime/60000;
				int sec=(int) (estimatedTime/1000)%60;
				gui.MessageAfterGame("Winner! \n Number of tries: "+ActualRow+"\n Time: "+min+":"+sec+"\n");
				gui.CleanUpGame();
			}
		} else {
			if (win) {
				SendScore();
				gui.RevealProblem(colors);
				long estimatedTime = System.currentTimeMillis()  - StartTime;
				int min=(int) estimatedTime/60000;
				int sec=(int) (estimatedTime/1000)%60;
				if (OtherPlayerScore != 42) { // Megvan a masik jatekos eredmenye
					if (ActualRow < OtherPlayerScore)
						gui.MessageAfterGame("Winner! \n Number of tries: "+ActualRow+"\n Time: "+min+":"+sec+"\n");
					else
						gui.MessageAfterGame("You lost! \n Number of tries: "+ActualRow+"\n Time: "+min+":"+sec+"\n");
				}
				// WAIT FOR OTHER PLAYER
				else {
					gui.MessageAfterGame("Waiting for the other player...");
					for (int i = 10; i > 0 && OtherPlayerScore == 42; i--) {

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (ActualRow <= OtherPlayerScore)
						gui.MessageAfterGame("Winner! \n Number of tries: "+ActualRow+"\n Time: "+min+":"+sec+"\n");		
					else
						gui.MessageAfterGame("You lost! \n Number of tries: "+ActualRow+"\n Time: "+min+":"+sec+"\n");	
				}
				gui.CleanUpGame();
				net.disconnect();
			}
			else if (ActualRow == 2 * colourNum) {
				SendScore();
				gui.RevealProblem(colors);
				long estimatedTime = System.currentTimeMillis()  - StartTime;
				int min=(int) estimatedTime/60000;
				int sec=(int) (estimatedTime/1000)%60;
				gui.MessageAfterGame("You lost! \n Number of tries: "+ActualRow+"\n Time: "+min+":"+sec+"\n");
				gui.CleanUpGame();
			}
		}
	}

	public void UserClick_ColorChooser(GUI.COLOR color) {
		pickedColour = color.ordinal();
		System.out.printf("\n %d", pickedColour);
		GUI.COLOR c = GUI.COLOR.values()[pickedColour];
		System.out.printf("\t %s", c);
	}

	/************************
	 * Dummy game functions *
	 ************************/

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
				gui.AddStick(ActualRow * 2, i, GUI.COLOR.values()[7]);// add a
																		// black
																		// stick
				black--;
			} else if (white > 0) {
				gui.AddStick(ActualRow * 2, i, GUI.COLOR.values()[8]);// add a
																		// white
																		// stick
				white--;
			}
		}
		for (int i = 0; i < 2; i++) {
			if (black > 0) {
				gui.AddStick(ActualRow * 2 + 1, i, GUI.COLOR.values()[7]);// add
																			// a
																			// black
																			// stick
				black--;
			} else if (white > 0) {
				gui.AddStick(ActualRow * 2 + 1, i, GUI.COLOR.values()[8]);// add
																			// a
																			// white
																			// stick
				white--;
			}
		}
		return win;
	}

	public void writeToFile(String filename, String highscore) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			writer.write(highscore);

		} catch (IOException e) {
			System.err.println(e.getCause());
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				System.err.println(e.getCause());
			}
		}
	}

	public String[] insertScore(Scanner in, String[] lines, int tryNum) {
		int[] lineScore = new int[10];
		for (int i = 0; i < 10; i++) {
			String[] parts = lines[i].split("@");
			// System.out.println(parts[0]+parts[1]);
			lineScore[i] = Integer.parseInt(parts[1]);
		}
		String[] newlines = new String[10];
		if (tryNum < lineScore[9]) {
			System.out.println("Ird be a neved:");
			String score = in.nextLine() + "@" + tryNum + "\n";
			for (int i = 0; i < 10; i++) {
				newlines[i] = lines[i] + "\n";
				if (tryNum < lineScore[i]) {
					newlines[i] = score;
					for (int j = i; j < 9; j++) {
						newlines[j + 1] = lines[j] + "\n";
					}
					return newlines;
				}
			}
		}
		return lines;
	}

	public String[] readFromFile(String filename) throws IOException {
		InputStream is = null;
		try {
			is = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		BufferedReader buf = new BufferedReader(new InputStreamReader(is));
		String[] lines = new String[11];
		int lineCntr = 0;
		String line = buf.readLine();
		while (line != null) {
			lines[lineCntr] = line;
			lineCntr++;
			line = buf.readLine();
		}
		return lines;
	}

	public void RefreshHighScore() throws IOException {
		Scanner in = new Scanner(System.in);
		System.out.printf("\n Gyõzelem! Próbálkozások száma: %d\n", ActualRow);
		String[] lines = new String[10];
		lines = readFromFile("highscore.txt");
		lines = insertScore(in, lines, ActualRow);
		String highscores = "";
		System.out.printf("\nHigh Scores\n");
		for (int i = 0; i < 10; i++) {
			highscores = highscores + lines[i];
			System.out.printf("%s\n", lines[i]);
		}
		writeToFile("Highscore.txt", highscores);
		System.out.printf("Vége.");
	}

	void StartServer() {
		if (net != null)
			net.disconnect();
		net = new SerialServer(this);
		net.connect("localhost");
	}

	void StartClient() {
		if (net != null)
			net.disconnect();
		net = new SerialClient(this);
		net.connect("localhost");
	}

	void SendProblem() {
		if (net == null)
			return;
		int [] pack=new int[5];
		for(int i=0;i<4;i++) pack[i]=problem[i];
		pack[4]=colourNum;
		net.send(pack);
	}

	void ReceivedProblem(int[] pack) {
		for(int i=0;i<4;i++) problem[i]=pack[i];
		colourNum = pack[4];
		ActualRow = 0;
		// TESZTELESRE
		System.out.printf("A feladat:\t");
		for (int i = 0; i < 4; i++)
			System.out.printf("%s\t", GUI.COLOR.values()[problem[i]]);
		System.out.println();
		
		gui.NumColors=colourNum;
		gui.StartGame(2*colourNum);
	}

	void ClientConnected() {
		problem = generateProblem(colourNum, colourRepeat);
		SendProblem();
		ActualRow = 0;
		// TESZTELESRE
		System.out.printf("A feladat:\t");
		for (int i = 0; i < 4; i++)
			System.out.printf("%s\t", GUI.COLOR.values()[problem[i]]);
		System.out.println();
		
		gui.StartGame(2*colourNum);
	}

	void ReceivedScore(int[] pack) {
		System.out.println("Recieved the other player's score. Waiting for you...");
		OtherPlayerScore = pack[0];
	}
	void SendScore() {
		System.out.println("Sending score");
		int[] pack=new int[5];//Kesobb bovitheto pl idovel
		pack[0]=ActualRow;
		for (int i = 1; i < 5; i++) pack[i]=0;
		net.send(pack);
		MyScoreSent=true;
	}	
}
