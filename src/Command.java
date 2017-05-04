import java.io.Serializable;

public class Command implements Serializable{
	public boolean dirtyBit;
	public String lastPressedButton; 	//Start1P, Start2P, Exit, ColorChooser, Dot, LineAccepter, Highscores
	public GUI.COLOR pickedColour;		//lastPressedButton=="ColorChooser"
	public int line;					//lastPressedButton=="Dot"
	public int column;					//lastPressedButton=="Dot"
	public int colourNum;				//lastPressedButton=="Settings_OK"
	public boolean colourRepeat;		//lastPressedButton=="Settings_OK"
	public String IP;					//lastPressedButton=="Start2P"
	public boolean server;				//lastPressedButton=="Start2P"
	public String Name;
	Command(){
		dirtyBit=false;
		lastPressedButton=null;
		pickedColour=GUI.COLOR.RED;
	}
}
