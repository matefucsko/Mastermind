import java.io.Serializable;

public class GameState implements Serializable{
	public GUI.COLOR[][] Dots;
	public GUI.COLOR[][] Sticks;
	public int[] problem;
	public int ActualRow;
	public int tryMax;
	public GUI.COLOR pickedColour;
	public int numColors;
	public String LastChanged; //=="Exit", "Board", "Settings", ...
	public boolean colourRepeat;
	GameState(){
		Dots = new GUI.COLOR[4][10];
		Sticks = new GUI.COLOR[2][20];
		problem = new int[4];
		numColors = 6;
		tryMax = 10;
		pickedColour= GUI.COLOR.RED;
		ActualRow=0;
		colourRepeat=false;
	}
	GameState(int DotColumns, int DotRows,int StickColumns, int StickRows){
		Dots = new GUI.COLOR[DotColumns][DotRows];
		Sticks = new GUI.COLOR[StickColumns][StickRows];
		problem = new int[4];
		numColors = 6;
		tryMax = DotRows;
		colourRepeat=false;
		pickedColour= GUI.COLOR.RED;
		ActualRow=0;
	}
	public void copyGameState(GameState other){
		this.Dots=other.Dots;
		this.Sticks=other.Sticks;
		this.problem = other.problem;
		this.ActualRow = other.ActualRow;
		this.tryMax = other.tryMax;
		this.pickedColour = other.pickedColour;
		this.LastChanged = other.LastChanged;
		this.numColors=other.numColors;
		this.colourRepeat=other.colourRepeat;
	}
}
