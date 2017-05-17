
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
	private boolean SinglePlayer;
	private int OtherPlayerScore;
	private long OtherPlayerTime;
	public boolean MyScoreSent;
	private long StartTime;
	private Command cmd;

	/*50ms-enk�nt lefut. 
	 * A GUI-t�l kapott utols� utas�t�s (cmd.lastPressedButton) alapj�n d�nti el, hogy mit kell csin�lnia.
	 * Ha a GUI-t�l nem kapott �j feladatot, a dirtyBit false �rt�k�, a sz�l visszamegy alv� �llapotba.*/
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
							System.out.printf("Start1P\n");
							Start1Player();
							break;
						case "Start2P":
							if (cmd.server) {
								gs.tryMax = colourNum * 2 - 2;
								System.out.printf("Start 2P Game - Server \n");
								Start2Player();
							} else {
								System.out.printf("Start 2P Game - Client \n");
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
							gs.Message=HighScore2HTML();
							gs.LastChanged="Highscores";
							gui.onNewGameState(gs);
							break;	
						case "Exit":
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
					Thread.sleep(50);
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
	//Egyj�t�kos menet ind�t�sa
	public void Start1Player() {
		SinglePlayer = true;
		gs.problem = generateProblem(colourNum, colourRepeat);
		gs.tryMax = colourNum * 2 - 2;		
		gs.ActualRow = 0;
		gs.LastChanged="Start";
		// TESZTELESRE
		System.out.printf("A feladat:\t");
		for (int i = 0; i < 4; i++)
			System.out.printf("%s\t", GUI.COLOR.values()[gs.problem[i]]);
		System.out.println();
		StartTime = System.currentTimeMillis();		
		gui.onNewGameState(gs);
	}
	//2j�t�kos ind�t�sa kliensk�nt
	public void Start2Player(String IP) {
		SinglePlayer = false;
		MyScoreSent = false;
		OtherPlayerScore = 42;//Ennyi pr�b�lkoz�s nem lehet, �gy ha enn�l kevesebbre v�ltozik az �rt�ke, az jelzi, hogy a m�sik j�t�kos m�r v�gzett.
		gs.ActualRow = 0;
		StartClient(IP);
	}
	//2j�t�kos ind�t�sa szerverk�nt
	public void Start2Player() {
		SinglePlayer = false;
		MyScoreSent = false;
		gs.ActualRow = 0;
		OtherPlayerScore = 42;
		StartServer();
	}
	/*Ha a j�t�kos a sort el akarja fogadtatni, a mellette l�v� checkboxra kattint. Ekkor h�v�dik meg ez a f�ggv�ny.
	 *Az aktu�lis sor sz�neit int-t� konvert�lja, megh�vaja az EvaluateTry() ki�rt�kel� f�ggv�nyt. 
	 *Ha a j�t�kos nyert, vagy kifutott a sorokb�l, sz�l a gui-nak, hogy 
	 *	1) Mutassa meg a feladott kombin�ci�t, amit ki kellett tal�lni
	 *	2) �rjon ki gratul�l�/game over �zenetet + j�t�kstatisztik�t egy �j ablakban.
	 *Egyj�t�kos gy�zelem eset�n megh�vja a CompareToHighScores() f�ggv�nyt, lsd. k�s�bb */
	public void UserClick_LineAccepter() throws IOException {
		int[] dots = new int[4];
		for (int i = 0; i < 4; i++) {
			if (gs.Dots[i][gs.ActualRow] == null)
				dots[i] = -1;
			else
				dots[i] = gs.Dots[i][gs.ActualRow].ordinal();
		}
		boolean win = EvaluateTry(colourNum, gs.problem, dots);
		gs.ActualRow++;
		gs.LastChanged="Board";
		gui.onNewGameState(gs);
		GUI.COLOR[] colors = new GUI.COLOR[4];
		for (int i = 0; i < 4; i++)
			colors[i] = GUI.COLOR.values()[gs.problem[i]];
		if (SinglePlayer) {
			if (win) {
				gs.LastChanged="Reveal";
				gui.onNewGameState(gs);
				long estimatedTime = System.currentTimeMillis() - StartTime;
				CompareToHighScores(estimatedTime);
				CleanUpGame();
			}
			else if (gs.ActualRow == gs.tryMax) {
				gs.LastChanged="Reveal";
				gui.onNewGameState(gs);
				long estimatedTime = System.currentTimeMillis() - StartTime;
				int min = (int) estimatedTime / 60000;
				int sec = (int) (estimatedTime / 1000) % 60;
				gs.Message="Vesztett�l! \n Pr�b�lkoz�said sz�ma: " + gs.ActualRow + "\n J�t�kid�: " + min + ":" + sec + "\n";
				gs.LastChanged="Message";		
				gui.onNewGameState(gs);	
				CleanUpGame();
			}
		} else {
			if (win) {
				SendScore();
				gs.LastChanged="Reveal";
				gui.onNewGameState(gs);
				long estimatedTime = System.currentTimeMillis() - StartTime;
				int min = (int) estimatedTime / 60000;
				int sec = (int) (estimatedTime / 1000) % 60;
				if (OtherPlayerScore != 42) { // Megvan a masik jatekos
												// eredmenye
					int min2 = (int) OtherPlayerTime / 60000;
					int sec2 = (int) (OtherPlayerTime / 1000) % 60;
					if(estimatedTime>OtherPlayerTime+30000)
						gs.Message= "Kifutott�l az id�b�l! \nEredm�nyed:                              " + gs.ActualRow + " pr�b�lkoz�s, " + min + ":" + sec + "\n"
								+"A m�sik j�t�kos eredm�nye: "+OtherPlayerScore+" pr�b�lkoz�s, "+min2+":"+sec2+"     \n";
					else if (gs.ActualRow < OtherPlayerScore)
						gs.Message=	"Gy�zt�l! \nEredm�nyed:                              " + gs.ActualRow + " pr�b�lkoz�s, " + min + ":" + sec + "\n"
								+"A m�sik j�t�kos eredm�nye: "+OtherPlayerScore+" pr�b�lkoz�s, "+min2+":"+sec2+"     \n";
					else
						gs.Message= "Vesztett�l! \nEredm�nyed:                              " + gs.ActualRow + " pr�b�lkoz�s, " + min + ":" + sec + "\n"
								+"A m�sik j�t�kos eredm�nye: "+OtherPlayerScore+" pr�b�lkoz�s, "+min2+":"+sec2+"     \n";
					gs.LastChanged="Message";	
					gui.onNewGameState(gs);	
				}
				// WAIT FOR OTHER PLAYER
				else {
					gs.Message="V�rakoz�s a m�sik j�t�kosra";
					gs.LastChanged="Message";	
					gui.onNewGameState(gs);	
					for (int i = 30; i > 0 && OtherPlayerScore == 42; i--) { //Max 30 s v�rakoz�s

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					int min2 = (int) OtherPlayerTime / 60000;
					int sec2 = (int) (OtherPlayerTime / 1000) % 60;
					if(OtherPlayerScore==42)
						gs.Message="Gy�zt�l! \n A m�sik j�t�kosnak lej�rt az ideje.\n Pr�b�lkoz�said sz�ma: " + gs.ActualRow + "\n J�t�kid�: " + min + ":" + sec + "\n";
					else{
						if (gs.ActualRow <= OtherPlayerScore)
							gs.Message=	"Gy�zt�l! \nEredm�nyed:                              " + gs.ActualRow + " pr�b�lkoz�s, " + min + ":" + sec + "\n"
									+"A m�sik j�t�kos eredm�nye: "+OtherPlayerScore+" pr�b�lkoz�s, "+min2+":"+sec2+"     \n";
						else
							gs.Message= "Vesztett�l! \nEredm�nyed:                              " + gs.ActualRow + " pr�b�lkoz�s, " + min + ":" + sec + "\n"
									+"A m�sik j�t�kos eredm�nye: "+OtherPlayerScore+" pr�b�lkoz�s, "+min2+":"+sec2+"     \n";
					}
					gs.LastChanged="Message";	
					gui.onNewGameState(gs);	
				}
				CleanUpGame();
				net.disconnect();
			} else if (gs.ActualRow == gs.tryMax) {
				SendScore();
				gs.LastChanged="Reveal";
				gui.onNewGameState(gs);
				long estimatedTime = System.currentTimeMillis() - StartTime;
				int min = (int) estimatedTime / 60000;
				int sec = (int) (estimatedTime / 1000) % 60;
				gs.Message="Vesztett�l! \n Pr�b�lkoz�said sz�ma: " + gs.ActualRow + "\n J�t�kid�: " + min + ":" + sec + "\n";
				gs.LastChanged="Message";	
				gui.onNewGameState(gs);	
				CleanUpGame();
			}
		}
	}
	/*4 random eg�sz sz�mot ad vissza 0 �s <colourNum> k�z�tt. Megadhat�, hogy legyen-e ism�tl�s <colourRepeat> �rt�k�vel.*/
	public int[] generateProblem(int colourNum, boolean colourRepeat) {
		int[] dots = new int[4];
		for (int i = 0; i < 4; i++) {
			double d = colourNum * Math.random();
			dots[i] = (int) Math.round(d - 0.5);
			if (colourRepeat == false) {
				for (int j = 0; j < i; j++) {
					if (dots[j] == dots[i]) {
						i--; // Ujra randomgener�lom az i. tagot
						break;
					}
				}
			}
			// System.out.printf("%d : %d\n",i,dots[i]);
		}
		return dots;
	}
	/*Ki�rt�keli a sort. gs-ben be�ll�tja a fekete-feh�r �rt�kel� p�ck�k �rt�k�t. Ha helyes volt a tipp �s a j�t�kos nyert, true-val t�r vissza */
	public boolean EvaluateTry(int colourNum, int[] problem, int[] proba) {
		int black = 0;
		int white = 0;
		boolean win;
		boolean[] evaluated = new boolean[4]; // a tippekb�l melyik van m�r
												// �rt�kelve
		for (int i = 0; i < 4; i++)
			evaluated[i] = false;
		boolean[] used = new boolean[4]; // a feladat adott p�cke m�r besz�mit
											// az �rt�kel�sbe
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
	/*Kiolvassa az adott be�ll�t�sokhoz tartoz� highscore f�jlt, majd form�zva belet�lti egy stringbe, amivel visszat�r.*/
	private String HighScore2HTML() throws UnsupportedEncodingException, FileNotFoundException, IOException{
		String content="";
		String filename;
		if(colourRepeat) filename = "highscore"+colourNum+"r.txt";
		else filename = "highscore"+colourNum+".txt";
			try {
				content = new String(Files.readAllBytes(Paths.get(filename)));
			} catch (IOException e) {
				makeHighScoreFiles(); //Ha s�r�ltek, vagy nincsenek meg a highscore f�jlok, �jragener�lja �ket
				content = new String(Files.readAllBytes(Paths.get(filename)));
				//e.printStackTrace();
			}
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
				 + "			<th>Helyez�s</th>"
				 + "			<th>N�v</b></th>"
				 + "			<th>Pr�b�lkoz�sok</b></th>"				 
				 + "			<th>Id�</b></th>"
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
	/*Kiolvassa az adott be�ll�t�sokhoz tartoz� highscore f�jlt, �sszehasonl�tja vele az aktu�lis eredm�nyt.
	 * A gs.Message-ben be�ll�tja az �zenetet, amit a GUI-nak ki kell �rnia, att�l f�gg�en, hogy beker�lt-e a top10-be a j�t�kos. 
	 * Ha igen, a GUI-nak "GetName"-t ad meg feladatk�nt, mire az bek�ri a j�t�kos nev�t, az adott �zenettel. 
	 * (K�l�nben csak a gratul�ci�t �s a statisztik�t jelen�ti meg a gui egy felugr� ablakban. Ez a sz�veg is gs.Message-ben ad�dik �t.)
	 * Ha megvan a n�v, megh�vja az insertScore f�ggv�nyt, ami elhelyezi az adott j�t�kos nev�t �s statisztik�it a megfelel� helyen a highscore adatokat tartalmaz� stringben.
	 * V�g�l ezt a stringet vissza�rja a highscore f�jlba.*/
	private void CompareToHighScores(long myTime) throws IOException {
		String content="";
		String filename;
		if(colourRepeat) filename ="highscore"+colourNum+"r.txt";
		else filename ="highscore"+colourNum+".txt";
		try {
			content = new String(Files.readAllBytes(Paths.get(filename)));
		} catch (IOException e) {
			makeHighScoreFiles(); //Ha s�r�ltek, vagy nincsenek meg a highscore f�jlok, �jragener�lja �ket
			content = new String(Files.readAllBytes(Paths.get(filename)));
			//e.printStackTrace();
		}
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
		int min = (int) myTime / 60000;
		int sec = (int) (myTime / 1000) % 60;
		if (place < 10) { // Felkerult a toplistara
			gs.Message="<html><center>Gratul�lunk!</center></html>"
					+ "\n Beker�lt�l a legjobb 10 k�z�!\n"
					+ "Pr�b�lkoz�sok sz�ma:\t"	+ (gs.ActualRow)
					+ "\nJ�t�kid�:\t" + min + ":" + sec + "\n"
					+ "Add meg a neved!";
			gs.LastChanged="GetName";
			gui.onNewGameState(gs);
			cmd.dirtyBit=false;
			String highscores = insertScore(cmd.Name, myTime, lines, place);
			try (Writer writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
				writer.write(highscores);
			}
		}
		else{//Nem kerult fel
			gs.Message="<html><center>Nyert�l!</center></html>"
					+ "Pr�b�lkoz�sok sz�ma:\t"	+ (gs.ActualRow)
					+ "\nJ�t�kid�:\t" + min + ":" + sec + "\n";
			gs.LastChanged="Message";
			gui.onNewGameState(gs);
		}
	}
	/*A nevet, j�t�kid�t �s pr�b�lkoz�ssz�mot besz�rja egy a highscore f�jloknak megfelel� form�tum� stringbe, a helyez�snek megfelel�en.*/
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
	/*Ha nincsenek, vagy s�r�ltek a highscore f�jlok, ez �jragener�lja �ket.
	 * Form�tum: 
	 * highscore<sz�nek sz�ma><r, ha van ism�tl�s>.txt
	 * 
	 * Minden sor:
	 * <n�v>@<pr�b�lkoz�sok sz�ma>@<j�t�kid�[ms]>#*/
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
	/*Az �sszes sz�nes p�ck�t kiszedi a j�t�kt�rb�l*/
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
	/*�j szervert ind�t*/
	void StartServer() {
		if (net != null)
			net.disconnect();
		net = new SerialServer(this);
		net.connect("localhost");
	}
	/*Kliensk�nt csatlakozik egy szerverhez IP c�m alapj�n*/
	void StartClient(String IP) {
		if (net != null)
			net.disconnect();
		net = new SerialClient(this);
		net.connect(IP);
	}
	/*5 tag� int t�mbk�nt elk�ldi a kliensnek a feladatot(sz�nek helyett sz�mok), illetve a sz�nek maxim�lis sz�m�t*/
	void SendProblem() {
		if (net == null)
			return;
		int[] pack = new int[5];
		for (int i = 0; i < 4; i++)
			pack[i] = gs.problem[i];
		pack[4] = colourNum;
		net.send(pack);
	}
	//Ha meg�rkezett a feladat a szervert�l, elmenti gs-be, majd sz�l a gui-nak, hogy friss�lt a gamestate �s az ind�thatja a j�t�kot
	void ReceivedProblem(int[] pack) {
		for (int i = 0; i < 4; i++)
			gs.problem[i] = pack[i];
		colourNum = pack[4];
		// TESZTELESRE
		System.out.printf("A feladat:\t");
		for (int i = 0; i < 4; i++)
			System.out.printf("%s\t", GUI.COLOR.values()[gs.problem[i]]);
		System.out.println();
		gs.tryMax = colourNum * 2 - 2;		
		gs.numColors = colourNum;
		gs.LastChanged="Start";
		gui.onNewGameState(gs);
		StartTime = System.currentTimeMillis();
	}
	//Ha csatlakozozz a kliens, a szerver gener�l egy feladatot, amit elk�ld a kliensnek. Friss�ti a saj�t gamestate-j�t, majd sz�l a gui-nak, hogy ind�thatja a j�t�kot
	void ClientConnected() {
		gs.problem = generateProblem(colourNum, colourRepeat);
		gs.tryMax = colourNum * 2 - 2;		
		gs.numColors = colourNum;
		gs.LastChanged="Start";
		SendProblem();
		// TESZTELESRE
		System.out.printf("A feladat:\t");
		for (int i = 0; i < 4; i++)
			System.out.printf("%s\t", GUI.COLOR.values()[gs.problem[i]]);
		System.out.println();
		gui.onNewGameState(gs);
		StartTime = System.currentTimeMillis();
	}
	//Ha a kliens vagy a szerver megkapta a m�sik j�t�kos eredm�ny�t (pr�b�lkoz�sok sz�m�t), elmenti azt.
	void ReceivedScore(int[] pack) {
		System.out.println("Recieved the other player's score. Waiting for you...");
		OtherPlayerScore = pack[0];
		OtherPlayerTime = System.currentTimeMillis() - StartTime;
	}
	/*Elk�ldi a pr�b�lkoz�sok sz�m�t egy int t�mbben (k�s�bbi b�v�t�sre), majd MyScoreSent-el jelzi, hogy � m�r v�gzett. 
	 * Ha a m�sik j�t�kost�l k�s�bb j�n eredm�ny �s az nem kevesebb pr�b�lkoz�s, akkor az nyer, aki hamarabb k�ldte.*/	
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
		cmd = new Command();
		Thread LogicThread = new Thread(new GameControlThread());
		LogicThread.start();
	}
}
