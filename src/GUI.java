import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.Panel;
import java.awt.Color;
import javax.swing.JInternalFrame;
import javax.swing.JTabbedPane;
import javax.swing.JDesktopPane;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;

import java.awt.CardLayout;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.List;
import java.awt.GridLayout;
import java.awt.Image;

import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JTextPane;
import javax.swing.JCheckBox;
import javax.swing.JProgressBar;
import javax.swing.Box;
import javax.swing.JLayeredPane;
import javax.swing.border.BevelBorder;
import javax.swing.JSlider;
import java.awt.Canvas;
import com.jgoodies.forms.factories.DefaultComponentFactory;
import javax.swing.event.AncestorListener;
import javax.swing.event.AncestorEvent;
import javax.swing.UIManager;


public class GUI extends JFrame {
	private Control ctrl;
	private Command C;
	private final int DotColumns_rel[] = {110, 145, 181, 215};
	private final int DotRows_rel[] = {89, 129, 170, 210, 250, 291, 333, 374, 413, 453, 493, 533, 573, 613, 653, 693, 733, 774, 813, 853};
	private final int StickColumns_rel[] = {53, 71};
	private final int StickRows_rel[] = {
			87,  103, 127, 143, 167, 183, 207, 223, 247, 263, 287, 303, 327, 343, 367, 383, 407, 423, 448, 464, 
			488, 504, 528, 544, 568, 584, 608, 624, 648, 664, 688, 704, 728, 744, 768, 784, 808, 824, 848, 864
			};	
	private final int DotHeight = 25;
	private final int DotWidth = 25;
	private final int StickHeight = 15;
	private final int StickWidth = 15;	
	private final int GUI_Height_Blank = 71;
	private final int GUI_Height_Menu = 337;
	
	public enum COLOR {
		RED, GREEN, DARKBLUE, ORANGE, YELLOW,  PURPLE, LIGHTBLUE, BLACK, WHITE, GRAY
	}
	public enum CLICKABLES
	{
		DOTS, COLORS, ACCEPT
	}
	
	private GameState gs;
	//private COLOR[][] gs.Dots = new COLOR[DotColumns_rel.length][DotRows_rel.length];
	//private COLOR[][] gs.Sticks = new COLOR[StickColumns_rel.length][StickRows_rel.length];
	private ArrayList<Component> TableObjects = new ArrayList<Component>();
	private ArrayList<Component> ColorChoosers = new ArrayList<Component>();
	private ArrayList<JCheckBox> LineAccepters = new ArrayList<JCheckBox>();
		
	private JPanel contentPane;
	private JPanel pnlMain;
	private JPanel pnlSettings;
	private JLayeredPane pnlGame;
	private JLabel Board;
	private JSlider Slider_NumColors;
	private JCheckBox chckbx_RepeatableColors;
	
	private JLabel CreateTable(int numRows)
	{
		JLabel Brd = new JLabel("");
		Brd.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) 
			{
				int x = arg0.getX();
				int y = arg0.getY();
				
				for(int line = 0; line < DotRows_rel.length; line++)
				{
					if(y > DotRows_rel[line] && y < (DotRows_rel[line] + DotHeight))
					{
						for(int column = 0; column < DotColumns_rel.length; column++)
						{
							if(x > DotColumns_rel[column] && x < (DotColumns_rel[column] + DotWidth))
							{
								//Interface function
								//ctrl.UserClick_gs.Dots(column, line); 
								C.column=column;
								C.line=line;
								C.lastPressedButton="Dot";
								ctrl.onCommand(C);
							}
						}
					}
					
				}
			}
		});
		
		ImageIcon icon = new ImageIcon(GUI.class.getResource("/Images/Board_" + numRows + ".png"));
		int Height = icon.getIconHeight();
		int Width = icon.getIconWidth();
		Brd.setIcon(icon);
		Brd.setBounds(10, 11, Width, Height);
		this.setBounds(this.getX(), this.getY(), this.getWidth(), Height + GUI_Height_Blank);
		return Brd;
	}
	
	private void RemakeTable(JLabel Brd)
	{		
		for(int i = 0; i< TableObjects.size(); i++)
		{
			pnlGame.remove(TableObjects.get(i));
		}
		TableObjects.clear();
		
		for(int line = 0; line < gs.tryMax; line++)
		{
			for(int column = 0; column < 4; column++)
			{
				if(gs.Dots[column][line] != null)
				{
					JLabel point = new JLabel("");
					point.setIcon(new ImageIcon(GUI.class.getResource("/Images/" + gs.Dots[column][line].name() + ".png")));
					point.setBounds(Brd.getX() + DotColumns_rel[column], Brd.getY() + DotRows_rel[line], DotHeight, DotWidth);
					TableObjects.add(point);
					pnlGame.add(point, 0);
				}
			}
		}
		
		for(int line = 0; line < gs.tryMax*2; line++)
		{
			for(int column = 0; column < StickColumns_rel.length; column++)
			{
				if(gs.Sticks[column][line] != null)
				{
					JLabel point = new JLabel("");
					point.setIcon(new ImageIcon(GUI.class.getResource("Images/STICK_" + gs.Sticks[column][line].name() + ".png")));
					point.setBounds(Brd.getX() + StickColumns_rel[column], Brd.getY() + StickRows_rel[line], StickHeight, StickWidth);
					TableObjects.add(point);
					pnlGame.add(point, 0);
				}
			}
		}
		

		pnlGame.repaint();
	}
	
	public void StartGame(int param_NumRows, int param_NumColors)
	{
		if(Board != null)
		{
			pnlGame.remove(Board);
		}			
		Board = CreateTable(param_NumRows);		
		pnlGame.add(Board, 1);
		
		for (int i = 0; i < ColorChoosers.size(); i++) {
			pnlGame.remove(ColorChoosers.get(i));
		}
		for (int i = 0; i < param_NumColors; i++) {
			pnlGame.add(ColorChoosers.get(i));
		}
		
		for(int i = 0; i < LineAccepters.size(); i++)
		{
			pnlGame.remove(LineAccepters.get(i));
		}
		for(int i = 0; i < param_NumRows; i++)
		{
			LineAccepters.get(i).setSelected(false);
			pnlGame.add(LineAccepters.get(i));
		}
		SetActiveLineAccepter(0);
		
		pnlMain.setVisible(false);
		pnlGame.setVisible(true);
	}
	
	public void onNewGameState(GameState gs){
		//TODO
		this.gs.copyGameState(gs);;
		switch (gs.LastChanged){
			case "Board":
				RemakeTable(Board);
				break;
			case "Exit":	
				BackToMain();
				break;
			case "Settings":
				break;
			default:
				System.out.println("WTF??");
		}
	}
	
/*	public void AddDot(int line, int column, COLOR color)
	{
		gs.Dots[column][line] = color;
		RemakeTable(Board);
	}
	public void RemoveDot(int line, int column)
	{
		gs.Dots[column][line] = null;
		RemakeTable(Board);
	}
	
	public void AddStick(int line, int column, COLOR color)
	{
		gs.Sticks[column][line] = color;
		RemakeTable(Board);
	}
	
	public void RemoveStick(int line, int column)
	{
		gs.Sticks[column][line] = null;
		RemakeTable(Board);
	}
*/
	public void RevealProblem(GUI.COLOR[] colors)
	{
		for(int i = 0; i < 4; i++)
		{
			JLabel point = new JLabel("");
			point.setIcon(new ImageIcon(GUI.class.getResource("/Images/" + colors[i].name() + ".png")));
			point.setBounds(Board.getX() + DotColumns_rel[i], Board.getY() + 30, DotHeight, DotWidth);
			TableObjects.add(point);
			pnlGame.add(point, 0);
		}
	}
	public String getName(String message){
		String Name = JOptionPane.showInputDialog(getParent(),
	            message, "");
		return Name;
	}

	public void MessageAfterGame(String message){	
		JOptionPane.showMessageDialog(this, message);
	}
	public void BackToMain(){
		pnlGame.setVisible(false);
		pnlMain.setVisible(true);
		setBounds(getX(), getY(), getWidth(), GUI_Height_Menu);
	}
	public void SetActiveLineAccepter(int param_index)
	{
		for(int i = 0; i < LineAccepters.size(); i++)
		{
			if(i < param_index)
			{
				LineAccepters.get(i).setVisible(true);
				LineAccepters.get(i).setEnabled(false);
			}
			else if(i > param_index)
			{
				LineAccepters.get(i).setVisible(false);
				LineAccepters.get(i).setEnabled(false);
			}
			else if(i == param_index)
			{
				LineAccepters.get(i).setVisible(true);
				LineAccepters.get(i).setEnabled(true);
			}
		}
	}
	
	public void ListHighScores(String HighScores)
	{
		JOptionPane.showMessageDialog(this, HighScores, "Toplista", JOptionPane.INFORMATION_MESSAGE);
		
	}
	
	/* ********************************************************************************************************
	 * **************************************Create the frame.*************************************************
	 * *******************************************************************************************************/
	public GUI(Control c) {
		ctrl = c;
		C=new Command();
		gs=new GameState();
		
		setFont(new Font("Dialog", Font.PLAIN, 12));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 30, 465, GUI_Height_Menu);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new CardLayout(0, 0));
		
		/* *************Main panel**************** */
		pnlMain = new JPanel();
		contentPane.add(pnlMain, "name_19339829389346");
		
		/* btn1Player */
		JButton btn1Player = new JButton("Egy j\u00E1t\u00E9kos");
		btn1Player.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				//Interface function: 
				C.lastPressedButton="Start1P";
				ctrl.onCommand(C);
			}
		});
		btn1Player.setBounds(77, 121, 104, 46);
		btn1Player.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		/* btn2Player */
		JButton btn2Player = new JButton("K\u00E9t j\u00E1t\u00E9kos");
		btn2Player.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				
				Object[] options = {"Start a new game",
                "Connect to game"};
				int NewGame = JOptionPane.showOptionDialog(getParent(),
					    "How would you like to play?",
					    "Client or host?",
					    JOptionPane.YES_NO_OPTION,
					    JOptionPane.QUESTION_MESSAGE,
					    null,     //do not use a custom Icon
					    options,  //the titles of buttons
					    options[0]); //default button title
				
				if(NewGame != 0)
				{/* Connect to Host */
					C.IP = JOptionPane.showInputDialog(getParent(),
                        "Please type in the host IP address!", "localhost");
					C.server=false;
				}
				else
					C.server=true;
				C.lastPressedButton="Start2P";
				ctrl.onCommand(C);
			}
		});
		btn2Player.setBounds(258, 121, 104, 46);
		
		/* btnSettings */
		JButton btnSettings = new JButton("Be\u00E1ll\u00EDt\u00E1sok");
		btnSettings.setBounds(77, 202, 104, 46);
		pnlMain.setLayout(null);
		pnlMain.add(btn1Player);
		pnlMain.add(btn2Player);
		pnlMain.add(btnSettings);
		btnSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pnlSettings.setVisible(true);
				pnlMain.setVisible(false);
			}
		});
		
		JButton btnHighScore = new JButton("Toplista");
		btnHighScore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//Dummy functionality
				/*String HighScores = 
						   "<html>"
						 + "	<table>"
						 + "		<tr> "
						 + "			<th>Helyezés</th>"
						 + "			<th>Név</b></th>"
						 + "			<th>Idõ</b></th>"
						 + "		</tr>"
						 + "		<tr>"
						 + "			<th style=\"font-weight: normal;\">1.</th>"
						 + "			<th style=\"font-weight: normal;\">Józsi1</th>"
						 + "			<th style=\"font-weight: normal;\">6:31</th>"
						 + "		</tr>"
						 + "		<tr>"
						 + "			<th style=\"font-weight: normal;\">2.</th>"
						 + "			<th style=\"font-weight: normal;\">Józsi2</th>"
						 + "			<th style=\"font-weight: normal;\">6:32</th>"
						 + "		</tr>"
						 + "		<tr>"
						 + "			<th style=\"font-weight: normal;\">3.</th>"
						 + "			<th style=\"font-weight: normal;\">Józsi3</th>"
						 + "			<th style=\"font-weight: normal;\">6:33</th>"
						 + "		</tr>"
						 + "		<tr>"
						 + "			<th style=\"font-weight: normal;\">4.</th>"
						 + "			<th style=\"font-weight: normal;\">Józsi4</th>"
						 + "			<th style=\"font-weight: normal;\">6:34</th>"
						 + "		</tr>"
						 + "		<tr>"
						 + "			<th style=\"font-weight: normal;\">5.</th>"
						 + "			<th style=\"font-weight: normal;\">Józsi5</th>"
						 + "			<th style=\"font-weight: normal;\">6:35</th>"
						 + "		</tr>"
						 + "	</table>"
						 + "</html>"
				;*/
				//ListHighScores(HighScores);
				C.lastPressedButton="Highscores";
				ctrl.onCommand(C);
				//Interface function
				//ctr.getHighScores();
			}
		});
		btnHighScore.setBounds(258, 202, 104, 46);
		pnlMain.add(btnHighScore);
		
		JTextPane txtpnTitle = new JTextPane();
		txtpnTitle.setFont(new Font("Tahoma", Font.BOLD, 30));
		txtpnTitle.setText("Mastermind j\u00E1t\u00E9k");
		txtpnTitle.setBounds(80, 34, 278, 46);
		txtpnTitle.setBackground(getBackground());
		pnlMain.add(txtpnTitle);


		
		/* *************Settings panel*****************/
		pnlSettings = new JPanel();
		contentPane.add(pnlSettings, "name_19370785543433");
		pnlSettings.setLayout(null);
		
		/* chckbx_RepeatableColors */
		chckbx_RepeatableColors = new JCheckBox("Repeatable colors");
		chckbx_RepeatableColors.setBounds(146, 192, 146, 23);
		pnlSettings.add(chckbx_RepeatableColors);
		
		/* Slider_NumColors */
		Slider_NumColors = new JSlider();
		Slider_NumColors.setToolTipText("");
		Slider_NumColors.setName("Difficulty");
		Slider_NumColors.setValueIsAdjusting(true);
		Slider_NumColors.setMinorTickSpacing(1);
		Slider_NumColors.setMajorTickSpacing(1);
		Slider_NumColors.setMinimum(4);
		Slider_NumColors.setMaximum(10);
		Slider_NumColors.setValue(6);
		Slider_NumColors.setPaintLabels(true);
		Slider_NumColors.setPaintTicks(true);
		Slider_NumColors.setBounds(131, 81, 177, 55);
		pnlSettings.add(Slider_NumColors);
		
		/* txtpnDifficulty */
		JTextPane txtpnDifficulty = new JTextPane();
		txtpnDifficulty.setBackground(UIManager.getColor("Button.background"));
		txtpnDifficulty.setEditable(false);
		txtpnDifficulty.setFont(new Font("Tahoma", Font.PLAIN, 13));
		txtpnDifficulty.setText("Difficulty (number of colors):");
		txtpnDifficulty.setBounds(131, 58, 177, 20);
		pnlSettings.add(txtpnDifficulty);
		
		/* btnOK */
		JButton btnOk = new JButton("OK");
		btnOk.setBounds(230, 260, 89, 23);
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				C.colourNum = Slider_NumColors.getValue();
				C.colourRepeat = chckbx_RepeatableColors.isSelected();
				C.lastPressedButton = "Settings_OK";
				ctrl.onCommand(C);
				pnlSettings.setVisible(false);
				pnlMain.setVisible(true);
			}
		});
		pnlSettings.add(btnOk);
		
		/* btnCancel */
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setBounds(131, 260, 89, 23);
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Slider_NumColors.setValue(gs.numColors);
				chckbx_RepeatableColors.setSelected(gs.colourRepeat);
				pnlSettings.setVisible(false);
				pnlMain.setVisible(true);
			}
		});
		pnlSettings.add(btnCancel);
		
		/* *************Game panel*****************/
		pnlGame = new JLayeredPane();
		contentPane.add(pnlGame, "name_151990897037164");
		
		/* btnExit */
		JButton btnExit = new JButton("Exit");
		btnExit.setBounds(356, 18, 62, 45);
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				//Interface function:
				C.lastPressedButton="Exit";
				ctrl.onCommand(C);			
			}
		});
		pnlGame.add(btnExit);
				
		/* Line Accepters */
		for(int i = 0; i < 20; i++)
		{
			final int index = i;
			JCheckBox chckbxNewCheckBox = new JCheckBox("");
			chckbxNewCheckBox.setBounds(301, DotRows_rel[i] + 10, 21, 23);
			chckbxNewCheckBox.setVisible(false);
			chckbxNewCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//Dummy Functionality
					SetActiveLineAccepter(index + 1);
					
					/*Interface Function*/
					C.lastPressedButton = "LineAccepter";
					ctrl.onCommand(C);
				}
			});
			LineAccepters.add(chckbxNewCheckBox);
		}
		
		/* Color Choosers */		
		final int PosY_ColorChoosers = 376;
		
		JLabel ColorChooserRED = new JLabel("");
		ColorChooserRED.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.RED;
				ctrl.onCommand(C);
			}
		});
		ColorChooserRED.setIcon(new ImageIcon(GUI.class.getResource("/Images/RED.png")));
		ColorChooserRED.setBounds(PosY_ColorChoosers, 105, 25, 25);
		ColorChoosers.add(ColorChooserRED);
		
		JLabel ColorChooserGREEN = new JLabel("");
		ColorChooserGREEN.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.GREEN;
				ctrl.onCommand(C);
			}
		});
		ColorChooserGREEN.setIcon(new ImageIcon(GUI.class.getResource("/Images/GREEN.png")));
		ColorChooserGREEN.setBounds(PosY_ColorChoosers, 141, 25, 25);
		ColorChoosers.add(ColorChooserGREEN);
		
		JLabel ColorChooserDARKBLUE = new JLabel("");
		ColorChooserDARKBLUE.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.DARKBLUE;
				ctrl.onCommand(C);
			}
		});
		ColorChooserDARKBLUE.setIcon(new ImageIcon(GUI.class.getResource("/Images/DARKBLUE.png")));
		ColorChooserDARKBLUE.setBounds(PosY_ColorChoosers, 177, 25, 25);
		ColorChoosers.add(ColorChooserDARKBLUE);
		

		JLabel ColorChooserORANGE = new JLabel("");
		ColorChooserORANGE.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.ORANGE;
				ctrl.onCommand(C);
			}
		});
		ColorChooserORANGE.setIcon(new ImageIcon(GUI.class.getResource("/Images/ORANGE.png")));
		ColorChooserORANGE.setBounds(PosY_ColorChoosers, 213, 25, 25);
		ColorChoosers.add(ColorChooserORANGE);
		
		JLabel ColorChooserYELLOW = new JLabel("");
		ColorChooserYELLOW.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.YELLOW;
				ctrl.onCommand(C);
			}
		});
		ColorChooserYELLOW.setIcon(new ImageIcon(GUI.class.getResource("/Images/YELLOW.png")));
		ColorChooserYELLOW.setBounds(PosY_ColorChoosers, 249, 25, 25);
		ColorChoosers.add(ColorChooserYELLOW);
		
		JLabel ColorChooserPURPLE = new JLabel("");
		ColorChooserPURPLE.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.PURPLE;
				ctrl.onCommand(C);
			}
		});
		ColorChooserPURPLE.setIcon(new ImageIcon(GUI.class.getResource("/Images/PURPLE.png")));
		ColorChooserPURPLE.setBounds(PosY_ColorChoosers, 285, 25, 25);
		ColorChoosers.add(ColorChooserPURPLE);
		
		JLabel ColorChooserLIGHTBLUE = new JLabel("");
		ColorChooserLIGHTBLUE.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.LIGHTBLUE;
				ctrl.onCommand(C);
			}
		});
		ColorChooserLIGHTBLUE.setIcon(new ImageIcon(GUI.class.getResource("/Images/LIGHTBLUE.png")));
		ColorChooserLIGHTBLUE.setBounds(PosY_ColorChoosers, 321, 25, 25);
		ColorChoosers.add(ColorChooserLIGHTBLUE);
		
		JLabel ColorChooserBLACK = new JLabel("");
		ColorChooserBLACK.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.BLACK;
				ctrl.onCommand(C);
			}
		});
		ColorChooserBLACK.setIcon(new ImageIcon(GUI.class.getResource("/Images/BLACK.png")));
		ColorChooserBLACK.setBounds(PosY_ColorChoosers, 357, 25, 25);
		ColorChoosers.add(ColorChooserBLACK);
		
		JLabel ColorChooserWHITE = new JLabel("");
		ColorChooserWHITE.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.WHITE;
				ctrl.onCommand(C);
			}
		});
		ColorChooserWHITE.setIcon(new ImageIcon(GUI.class.getResource("/Images/WHITE.png")));
		ColorChooserWHITE.setBounds(PosY_ColorChoosers, 393, 25, 25);
		ColorChoosers.add(ColorChooserWHITE);
		
		JLabel ColorChooserGRAY = new JLabel("");
		ColorChooserGRAY.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				//Interface Function
				C.lastPressedButton="ColorChooser";
				C.pickedColour=GUI.COLOR.GRAY;
				ctrl.onCommand(C);
			}
		});
		ColorChooserGRAY.setIcon(new ImageIcon(GUI.class.getResource("/Images/GRAY.png")));
		ColorChooserGRAY.setBounds(PosY_ColorChoosers, 429, 25, 25);
		ColorChoosers.add(ColorChooserGRAY);
				
	}
}
