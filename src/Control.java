
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

	/*50ms-enként lefut. 
	 * A GUI-tól kapott utolsó utasítás (cmd.lastPressedButton) alapján dönti el, hogy mit kell csinálnia.
	 * Ha a GUI-tól nem kapott új feladatot, a dirtyBit false értékû, a szál visszamegy alvó állapotba.*/
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
	//Egyjátékos menet indítása
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
	//2játékos indítása kliensként
	public void Start2Player(String IP) {
		SinglePlayer = false;
		MyScoreSent = false;
		OtherPlayerScore = 42;//Ennyi próbálkozás nem lehet, így ha ennél kevesebbre változik az értéke, az jelzi, hogy a másik játékos már végzett.
		gs.ActualRow = 0;
		StartClient(IP);
	}
	//2játékos indítása szerverként
	public void Start2Player() {
		SinglePlayer = false;
		MyScoreSent = false;
		gs.ActualRow = 0;
		OtherPlayerScore = 42;
		StartServer();
	}
	/*Ha a játékos a sort el akarja fogadtatni, a mellette lévõ checkboxra kattint. Ekkor hívódik meg ez a függvény.
	 *Az aktuális sor színeit int-té konvertálja, meghívaja az EvaluateTry() kiértékelõ függvényt. 
	 *Ha a játékos nyert, vagy kifutott a sorokból, szól a gui-nak, hogy 
	 *	1) Mutassa meg a feladott kombinációt, amit ki kellett találni
	 *	2) Írjon ki gratuláló/game over üzenetet + játékstatisztikát egy új ablakban.
	 *Egyjátékos gyõzelem esetén meghívja a CompareToHighScores() függvényt, lsd. késõbb */
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
				gs.Message="Vesztettél! \n Próbálkozásaid száma: " + gs.ActualRow + "\n Játékidõ: " + min + ":" + sec + "\n";
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
						gs.Message= "Kifutottál az idõbõl! \nEredményed:                              " + gs.ActualRow + " próbálkozás, " + min + ":" + sec + "\n"
								+"A másik játékos eredménye: "+OtherPlayerScore+" próbálkozás, "+min2+":"+sec2+"     \n";
					else if (gs.ActualRow < OtherPlayerScore)
						gs.Message=	"Gyõztél! \nEredményed:                              " + gs.ActualRow + " próbálkozás, " + min + ":" + sec + "\n"
								+"A másik játékos eredménye: "+OtherPlayerScore+" próbálkozás, "+min2+":"+sec2+"     \n";
					else
						gs.Message= "Vesztettél! \nEredményed:                              " + gs.ActualRow + " próbálkozás, " + min + ":" + sec + "\n"
								+"A másik játékos eredménye: "+OtherPlayerScore+" próbálkozás, "+min2+":"+sec2+"     \n";
					gs.LastChanged="Message";	
					gui.onNewGameState(gs);	
				}
				// WAIT FOR OTHER PLAYER
				else {
					gs.Message="Várakozás a másik játékosra";
					gs.LastChanged="Message";	
					gui.onNewGameState(gs);	
					for (int i = 30; i > 0 && OtherPlayerScore == 42; i--) { //Max 30 s várakozás

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					int min2 = (int) OtherPlayerTime / 60000;
					int sec2 = (int) (OtherPlayerTime / 1000) % 60;
					if(OtherPlayerScore==42)
						gs.Message="Gyõztél! \n A másik játékosnak lejárt az ideje.\n Próbálkozásaid száma: " + gs.ActualRow + "\n Játékidõ: " + min + ":" + sec + "\n";
					else{
						if (gs.ActualRow <= OtherPlayerScore)
							gs.Message=	"Gyõztél! \nEredményed:                              " + gs.ActualRow + " próbálkozás, " + min + ":" + sec + "\n"
									+"A másik játékos eredménye: "+OtherPlayerScore+" próbálkozás, "+min2+":"+sec2+"     \n";
						else
							gs.Message= "Vesztettél! \nEredményed:                              " + gs.ActualRow + " próbálkozás, " + min + ":" + sec + "\n"
									+"A másik játékos eredménye: "+OtherPlayerScore+" próbálkozás, "+min2+":"+sec2+"     \n";
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
				gs.Message="Vesztettél! \n Próbálkozásaid száma: " + gs.ActualRow + "\n Játékidõ: " + min + ":" + sec + "\n";
				gs.LastChanged="Message";	
				gui.onNewGameState(gs);	
				CleanUpGame();
			}
		}
	}
	/*4 random egész számot ad vissza 0 és <colourNum> között. Megadható, hogy legyen-e ismétlés <colourRepeat> értékével.*/
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
	/*Kiértékeli a sort. gs-ben beállítja a fekete-fehér értékelõ pöckök értékét. Ha helyes volt a tipp és a játékos nyert, true-val tér vissza */
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
	/*Kiolvassa az adott beállításokhoz tartozó highscore fájlt, majd formázva beletölti egy stringbe, amivel visszatér.*/
	private String HighScore2HTML() throws UnsupportedEncodingException, FileNotFoundException, IOException{
		String content="";
		String filename;
		if(colourRepeat) filename = "highscore"+colourNum+"r.txt";
		else filename = "highscore"+colourNum+".txt";
			try {
				content = new String(Files.readAllBytes(Paths.get(filename)));
			} catch (IOException e) {
				makeHighScoreFiles(); //Ha sérültek, vagy nincsenek meg a highscore fájlok, újragenerálja õket
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
	/*Kiolvassa az adott beállításokhoz tartozó highscore fájlt, összehasonlítja vele az aktuális eredményt.
	 * A gs.Message-ben beállítja az üzenetet, amit a GUI-nak ki kell írnia, attól függõen, hogy bekerült-e a top10-be a játékos. 
	 * Ha igen, a GUI-nak "GetName"-t ad meg feladatként, mire az bekéri a játékos nevét, az adott üzenettel. 
	 * (Különben csak a gratulációt és a statisztikát jeleníti meg a gui egy felugró ablakban. Ez a szöveg is gs.Message-ben adódik át.)
	 * Ha megvan a név, meghívja az insertScore függvényt, ami elhelyezi az adott játékos nevét és statisztikáit a megfelelõ helyen a highscore adatokat tartalmazó stringben.
	 * Végül ezt a stringet visszaírja a highscore fájlba.*/
	private void CompareToHighScores(long myTime) throws IOException {
		String content="";
		String filename;
		if(colourRepeat) filename ="highscore"+colourNum+"r.txt";
		else filename ="highscore"+colourNum+".txt";
		try {
			content = new String(Files.readAllBytes(Paths.get(filename)));
		} catch (IOException e) {
			makeHighScoreFiles(); //Ha sérültek, vagy nincsenek meg a highscore fájlok, újragenerálja õket
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
			gs.Message="<html><center>Gratulálunk!</center></html>"
					+ "\n Bekerültél a legjobb 10 közé!\n"
					+ "Próbálkozások száma:\t"	+ (gs.ActualRow)
					+ "\nJátékidõ:\t" + min + ":" + sec + "\n"
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
			gs.Message="<html><center>Nyertél!</center></html>"
					+ "Próbálkozások száma:\t"	+ (gs.ActualRow)
					+ "\nJátékidõ:\t" + min + ":" + sec + "\n";
			gs.LastChanged="Message";
			gui.onNewGameState(gs);
		}
	}
	/*A nevet, játékidõt és próbálkozásszámot beszúrja egy a highscore fájloknak megfelelõ formátumú stringbe, a helyezésnek megfelelõen.*/
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
	/*Ha nincsenek, vagy sérültek a highscore fájlok, ez újragenerálja õket.
	 * Formátum: 
	 * highscore<színek száma><r, ha van ismétlés>.txt
	 * 
	 * Minden sor:
	 * <név>@<próbálkozások száma>@<játékidõ[ms]>#*/
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
	/*Az összes színes pöcköt kiszedi a játéktérbõl*/
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
	/*Új szervert indít*/
	void StartServer() {
		if (net != null)
			net.disconnect();
		net = new SerialServer(this);
		net.connect("localhost");
	}
	/*Kliensként csatlakozik egy szerverhez IP cím alapján*/
	void StartClient(String IP) {
		if (net != null)
			net.disconnect();
		net = new SerialClient(this);
		net.connect(IP);
	}
	/*5 tagú int tömbként elküldi a kliensnek a feladatot(színek helyett számok), illetve a színek maximális számát*/
	void SendProblem() {
		if (net == null)
			return;
		int[] pack = new int[5];
		for (int i = 0; i < 4; i++)
			pack[i] = gs.problem[i];
		pack[4] = colourNum;
		net.send(pack);
	}
	//Ha megérkezett a feladat a szervertõl, elmenti gs-be, majd szól a gui-nak, hogy frissült a gamestate és az indíthatja a játékot
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
	//Ha csatlakozozz a kliens, a szerver generál egy feladatot, amit elküld a kliensnek. Frissíti a saját gamestate-jét, majd szól a gui-nak, hogy indíthatja a játékot
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
	//Ha a kliens vagy a szerver megkapta a másik játékos eredményét (próbálkozások számát), elmenti azt.
	void ReceivedScore(int[] pack) {
		System.out.println("Recieved the other player's score. Waiting for you...");
		OtherPlayerScore = pack[0];
		OtherPlayerTime = System.currentTimeMillis() - StartTime;
	}
	/*Elküldi a próbálkozások számát egy int tömbben (késõbbi bõvítésre), majd MyScoreSent-el jelzi, hogy õ már végzett. 
	 * Ha a másik játékostól késõbb jön eredmény és az nem kevesebb próbálkozás, akkor az nyer, aki hamarabb küldte.*/	
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
