//	https://www3.ntu.edu.sg/home/ehchua/programming/java/J8d_Game_Framework.html
//	http://www.java-gaming.org/topics/basic-game/21919/view.html
//https://gamedev.stackexchange.com/questions/53705/how-can-i-make-a-sprite-sheet-based-animation-system

/*





	HOLY SHIT SHUT THE FUCK UP BITCH!! STOP BEING LITTLE BITCH BOY AND JUST DO IT MY GOD! Dont give a fuck what your head says. do it. bitch. 





*/

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.sound.sampled.*;
import javax.swing.JButton;
import java.awt.image.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.Thread;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;


//creates JFrame and adds game to it.

public class Platform{
	public static void main(String args[]){	 
		JFrame frame = new JFrame("Block Platformer");
		frame.setContentPane(new Game());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

}
//Main game class. This is where player is created, levels are loaded, game is drawn and updated, and collision detetion

class Game extends JPanel{

static final int WIDTH = 900;
static final int HEIGHT = 900;
static final int FPS = 60;
static final long UPDATES_NSEC = 1000000000L / FPS;

static enum GameState{INITIALIZED, PLAYING, PAUSED, GAMEOVER}	//control gamestate 

static GameState state;

private GamePanel canvas;

private Map mapperoni = new Map();

public int currentLevelNum = 0;

private double GRAVITY = .8;

private Player player;
private Terrain ground;
private ObjectManager om;
private ImageLoader il;
private AudioPlayer audioplayer;

private BufferedImage gameOverImage;
	Game(){	
		init();
		
		canvas = new GamePanel();
		canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		add(canvas);
	}
	public void init(){	//Create Level and load level 1. 
		System.out.println("Game Launched");
		
		il = new ImageLoader();
		gameOverImage = il.loadImg("./images/win.png");
		audioplayer = new AudioPlayer();
		om = new ObjectManager();
		
		
		player = new Player(300, 400);
		gameLoadLevel();
		gameStart();
	}
	public void gameStart(){	//start gameloop
		Thread thread = new Thread(){
			@Override
			public void run(){
				gameLoop();
			}
		};
		thread.start();
	}
	public void gameLoop(){	//60FPS loop

		state = GameState.PLAYING;

		long beginTime, elapsedTime, timeDifference;
		while(true){
			beginTime = System.nanoTime();

			if(state == GameState.PLAYING){
				gameUpdate();
			}
			repaint();

			elapsedTime = System.nanoTime() - beginTime;
			timeDifference = (UPDATES_NSEC - elapsedTime) / 1000000;
			
			if(timeDifference < 10){
				timeDifference = 10;
			}
			try{
				Thread.sleep(timeDifference);
			}
			catch(InterruptedException e){}
		}
	}
	public void gameUpdate(){ //update players, enemies, gravity, and collision detetion 
	
		if(om.wonGame){
			gameWin();	
		}
	
		player.update();	
		checkLevel();
		for(int i = 0; i < mapperoni.tile.length; i++){
			for(int j = 0; j < mapperoni.tile[0].length; j++){
				if(om.checkCollision(player, mapperoni.tile[i][j])){
					om.collide(player, mapperoni.tile[i][j]);
				}
			}
		}
		
		player.speedY += GRAVITY;
		
	}
	public void gameDraw(Graphics2D g2){	//draw everything within here. 
		
		if(state == GameState.GAMEOVER){
			g2.drawImage(gameOverImage, 0, 0, null);
			return;
		}
		
		mapperoni.drawMap(g2);
	
		setBackground(new Color(132, 216, 255)); 
		player.drawPlayer(g2);
		for(int i = 0; i < Terrain.terrainList.size(); i++){
			Terrain.terrainList.get(i).drawTerrain(g2);
		}
		for(int i = 0; i < Entity.entityList.size(); i++){
			Entity.entityList.get(i).drawEntity(g2);
		}
		g2.setColor(new Color(205, 215, 253));
		g2.fill3DRect(680, 50, 120, 30, true);
		
		g2.setColor(new Color(12, 19, 48));
		g2.setFont(new Font("Arial", Font.BOLD, 20));
		g2.drawString("Level - " + currentLevelNum, 700, 70);
	}
	public void gameLoadLevel(){	//Level loader
		nextLevel();
	}
	public void nextLevel(){
		
		audioplayer.playSound("./audio/passed.wav");
		
		currentLevelNum +=1;
		mapperoni.setMapID("levels/level" + currentLevelNum + ".txt");
		mapperoni.createMap();
		
		player.spawnX = mapperoni.playerSpawnX;
		player.spawnY = mapperoni.playerSpawnY;
		player.respawn();
	}
	public void previousLevel(){
		
		
		currentLevelNum -=1;
		
		if(currentLevelNum < 0){
			currentLevelNum = 0;
		}
		mapperoni.setMapID("levels/level" + currentLevelNum + ".txt");
		mapperoni.createMap();
		
		player.spawnX = mapperoni.playerSpawnX;
		player.spawnY = mapperoni.playerSpawnY;
		player.respawn();
	}
	public void checkLevel(){
		if(om.playerDied){
			om.playerDied = false;
			player.die();
			previousLevel();
		}
		if(player.y > 1000){
			player.die();
			previousLevel();
		}
		if(om.passedLevel){
			om.passedLevel = false;
			nextLevel();
		}
	}
	public void gameWin(){
		
		state = GameState.GAMEOVER;
		if(!audioplayer.gameOverPlayed && state == GameState.GAMEOVER){
			audioplayer.playGameOver();	
		}
	}
	class GamePanel extends JPanel implements KeyListener{	//Key Listener
	
		JPanel pauseMenu;
		JButton resume;
		JButton restart;
		JButton quit;
		
		public GamePanel(){
			setFocusable(true);
			requestFocus();
			addKeyListener(this);
			createPauseMenu();
		}
		@Override
		public void paintComponent(Graphics g){

			Graphics2D g2 = (Graphics2D) g;
			gameDraw(g2);
		}
		public void createPauseMenu(){
			
			pauseMenu = new JPanel();
			pauseMenu.setSize(5, 500);
			this.add(pauseMenu);
			pauseMenu.setVisible(false);
			
			
			resume = new JButton("Resume");
			resume.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					resumeGame();
				} 
			} );
			pauseMenu.add(resume);
			
			restart = new JButton("Restart");
			restart.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					restartGame();
				} 
			} );
			pauseMenu.add(restart);
			
			quit = new JButton("Quit");
			quit.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					quitGame();
				} 
			} );
			pauseMenu.add(quit);
		
		}
		public void showPauseMenu(){
			pauseMenu.setVisible(true);
		}
		public void hidePauseMenu(){
			pauseMenu.setVisible(false);
		}
		public void resumeGame(){
			hidePauseMenu();
			state = GameState.PLAYING;
		}
		public void restartGame(){
			state = GameState.PLAYING;
			audioplayer.gameOverPlayed = false;
			om.wonGame = false;
			hidePauseMenu();
			currentLevelNum = 0;
			nextLevel();
		}
		public void quitGame(){
			hidePauseMenu();
			System.exit(1);
		}
		
		@Override
		public void keyPressed(KeyEvent e){
			if(e.getKeyCode() == KeyEvent.VK_A){
				player.left = true;
			}
			if(e.getKeyCode() == KeyEvent.VK_D){
				player.right = true;
			}
			if(e.getKeyCode() == KeyEvent.VK_W){
				player.jump();
			}
			if(e.getKeyCode() == KeyEvent.VK_ESCAPE){ 
				if(state == GameState.PAUSED){
					hidePauseMenu();
					state = GameState.PLAYING;
					return;
				}
				showPauseMenu();
				state = GameState.PAUSED;
			}
		}
		@Override
		public void keyReleased(KeyEvent e){
			if(e.getKeyCode() == KeyEvent.VK_A){
				player.left = false;
			}
			if(e.getKeyCode() == KeyEvent.VK_D){
				player.right = false;
			}
		}
		@Override
		public void keyTyped(KeyEvent e){}
	}
}
class Player{	//Player class

private final int JUMP_POWER = -15;

public double x;
public double y;
public int sizeX;
public int sizeY;

public int spawnX = 0;
public int spawnY = 0;

public double centerX;
public double centerY;
private double speedX = 5;
public double speedY = .8;
public boolean isDead;

public boolean left = false;
public boolean right = false;
public boolean canJump = true;

AudioPlayer audioplayer = new AudioPlayer();

	public Player(double x, double y){
		this.x = x;
		this.y = y;
		this.sizeX = 45;
		this.sizeY = 45;
		
		this.isDead = false;
	} 
	public void drawPlayer(Graphics g2){	//draws player and HUD
		g2.setColor(Color.RED);
		g2.fillRect((int)x, (int)y, sizeX, sizeY);
		
	}
	public void update(){	//update position

		this.centerX = x + sizeX /2;
		this.centerY = y + sizeY /2;
		y += speedY;

		if(isDead){
			return;
		}
		if(left){
			x -= speedX;	
		}	
		if(right){
			x += speedX;	
		}
	}
	public void jump(){
		if(canJump && !isDead){
			audioplayer.playSound("./audio/jump.wav");
			canJump = false;
			speedY = JUMP_POWER;
		}
	}
	public void die(){
		isDead = true;
		audioplayer.playSound("./audio/death.wav");
		respawn();
	}
	public void respawn(){	//FIX: rework to not change sizeX and sizeY; change dimensions of hurtbox instead
		isDead = false;
		x = spawnX;
		y = spawnY;
	}
}

class Terrain{	//All terrain such as ground, buildings, and other blocks

public int x;
public int y;
public int sizeX;
public int sizeY;
public double centerX;
public double centerY;
private Color color;
public static List<Terrain> terrainList = new ArrayList<Terrain>();

	public Terrain(int x, int y, int sizeX, int sizeY, Color color){
		this.x = x;
		this.y = y;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.centerX = x + sizeX / 2;
		this.centerY = y + sizeY / 2;
		this.color = color;
		
		terrainList.add(this);
	}
	public void drawTerrain(Graphics g2){	//draws terrain
		g2.setColor(color);
		g2.fillRect(x, y, sizeX, sizeY);
	}
	public static void clearTerain(){
		 Terrain.terrainList.clear();
	}
}

class ObjectManager{	//Collision detetion and response. Moved here to clear up space in main game update method. 
public boolean passedLevel = false;
public boolean playerDied = false;
public boolean wonGame = false;
	public ObjectManager(){}
	
	public boolean checkCollision(Player player, Tile tile){
		if(player.x + player.sizeX > tile.x && player.x < tile.x + tile.sizeX && player.y + player.sizeY > tile.y && player.y < tile.y + tile.sizeY){
			
			return true;
		}
		else{
			return false;
		}
	}
	public void collide(Player player, Tile tile){	//FIX: make it better idfk
		double vecX = player.centerX - tile.centerX;
		double vecY = player.centerY - tile.centerY;
		
		switch(tile.id){ //return if not ground tile; dont collide. Die if touch orange lava tile. 
			case 0:
				return;
			case 1:
				break;
			case 2:
				return;
			case 3:
				return;
			case 4:
				return;
			case 5:
				return;
			case 6:
				playerDied = true;
				return;
			case 7:
				passedLevel = true;
				return;
			case 8:
				return;
			case 9:
				return;
			case 10:
				wonGame = true;
				return;
		}

		if( vecX * vecX > vecY * vecY ){
			if (vecX < 0){
				player.x = tile.x - player.sizeX;
			}
			if(vecX > 0){
				player.x = tile.x + tile.sizeX;
			}
		}
		
		if(vecY * vecY > vecX * vecX){
			if(vecY > 0){
				if(player.speedY < 0){
					//player.speedY = 0;
					player.y = tile.y + tile.sizeY;	
				}
			}
			else if (vecY < 0){
				if(player.speedY > 0){
				player.speedY = 0;
				player.canJump = true;
				player.y = tile.y - player.sizeY;
				}
			}
		}
	}
}
class FileManager{
public Scanner reader;

	public FileManager(){}
	public File setFile(File file){
		return file;
	}
	public int[][] readFile(String filename){
		
		int rows = 20;
		int colums = 20;
		
		int array[][] = new int[rows][colums];
		try{
			reader = new Scanner(new File(filename));
		}
		catch(FileNotFoundException e){
			JOptionPane.showMessageDialog(null, "Programmer fucked up: \n" + e);
			System.out.println("Aw fuck:" + e);
			System.exit(0);
		}		
		while(reader.hasNextLine()){
			for(int i = 0; i < array.length; i++){
			String[] line = reader.nextLine().trim().split(" ");	
				for(int j = 0; j < array.length; j++){
					array[i][j] = Integer.parseInt(line[j]);
				}
			}
		}
		return array;
	}
}


class ImageLoader{
	
    BufferedImage img;
    
    public BufferedImage loadImg(String s) {
        try {
            return ImageIO.read(new File(s));
        }
        catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    }
}

class AudioPlayer{
	public Clip clip;
	public boolean gameOverPlayed = false;
    public void playSound(String pathname) {
		
        File file = new File(pathname);
		
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        }
        catch (UnsupportedAudioFileException ex) {
            System.out.println("You fucked up");
            ex.printStackTrace();
        }
		catch(LineUnavailableException e){
			e.printStackTrace();
		}
		catch(IOException e){
			e.printStackTrace();
		}
    }
	public void playGameOver(){
		
		playSound("./audio/blockWin.wav");
		gameOverPlayed = true;
		
		clip.addLineListener(new LineListener(){
			public void update(LineEvent e){
				if(e.getType() == LineEvent.Type.STOP){
					clip.setFramePosition(0);
					clip.start();
				}
			}
		});
	}
}

class ObjectBox{
	
public double x;
public double y;
public int sizeX;
public int sizeY;
public Color color;
public boolean isActive;
public String type;

	public ObjectBox(int x, int y, int sizeX, int sizeY, String type){
		this.x = x;
		this.y = y;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.isActive = false;
	}
	public void activate(){
		this.color = Color.RED;
		this.isActive = true;
	}
	public void deactivate(){
		this.color = Color.BLACK;
		this.isActive = false;
	}
	public void drawBox(Graphics g2){
		g2.setColor(color);
		g2.drawRect((int)x, (int)y, sizeX, sizeY);
	}
	public void setX(int x){
			this.x = x;
	}
	public void setY(int y){
		this.y = y;
	}
}

class Tile{
public int x;
public int y;
public int sizeX;
public int sizeY;
public Color color;
public int id;
public double centerX;
public double centerY;
	Tile(int x, int y, int id){
		this.x = x;
		this.y = y;
		this.sizeX = 45;
		this.sizeY = 45;
		this.color = color;	
		this.id = id;
		this.centerX = x + sizeX / 2;
		this.centerY = y + sizeY / 2;
	}
}

class Map{
public String currentLevel;
public FileManager filemanager = new FileManager();
public  int mapID[][] =  {{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,5,5,5,5},	
						  {0,0,0,0,0,0,0,0,0,0,0,0,0,5,5,5,5,5,5,5},	
						  {0,2,2,0,0,0,0,0,0,0,0,0,5,0,0,5,5,5,5,5},      
						  {0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,5,5,5,5,5},
						  {0,0,0,0,0,2,2,2,2,0,0,0,0,0,5,0,0,5,0,0},
						  {0,0,0,0,0,0,0,0,2,2,2,2,0,5,0,0,0,5,0,0}, 
						  {0,0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,5,0,0,0},
						  {0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
						  {0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
						  {0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,2,2,0},
						  {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
						  {0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0,0,1,0,0},	
						  {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0,0},	
						  {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,0},	
						  {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},	
						  {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},	
						  {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},	
						  {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},	
						  {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4},	
						  {4,4,4,-4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4}};
public  int tileX = mapID.length;
public  int tileY = mapID[0].length;

public int playerSpawnX = 0;
public int playerSpawnY = 0;

public  Tile tile[][] = new Tile[tileX][tileY];
	public Map(){
		this.currentLevel = "level1.txt";
	}
	public  void createMap(){
		for(int i = 0; i < mapID.length; i++){
			for(int j = 0; j < mapID[0].length; j++){
				tile[i][j] = new Tile(j*45, i*45,mapID[i][j]);
				
				if(tile[i][j].id == 9){
				
					playerSpawnX = tile[i][j].x;
					playerSpawnY = tile[i][j].y;	
					
				}
			}
		}
	}
	public void setMapID(String filename){
		try{
			this.mapID = filemanager.readFile(filename);
		}
		catch(Exception e){
			System.out.println("aw fuck" + e);
		}
	}
	public Map getMap(){
		return this;
	}
	public void drawMap(Graphics g2){
		for(int i = 0; i < mapID.length; i++){
			for(int j = 0; j < mapID[0].length; j++){		
				mapSetColor();
				g2.setColor(tile[i][j].color);
				g2.fillRect(tile[i][j].x, tile[i][j].y, tile[i][j].sizeX, tile[i][j].sizeY);
			}
		}
	}
	public  void mapSetColor(){
		for(int i = 0; i < mapID.length; i++){
			for(int j = 0; j < mapID[0].length; j++){			
			
				if(tile[i][j].id == 0){				
					tile[i][j].color = new Color(150, 234, 255);	
				}
				if(tile[i][j].id == 1){
					tile[i][j].color = new Color(34, 155, 84);	
				}

				if(tile[i][j].id == 2){
					tile[i][j].color = Color.WHITE;	
				}

				if(tile[i][j].id == 3){
					tile[i][j].color = Color.RED;	
				}
				if(tile[i][j].id == 4){
					tile[i][j].color = new Color(34, 155, 84);
				}
				if(tile[i][j].id == 5){
					tile[i][j].color = Color.YELLOW;	
				}
				if(tile[i][j].id == 6){
					tile[i][j].color = Color.ORANGE;	
				}
				if(tile[i][j].id == 7){
					tile[i][j].color = Color.PINK;
				}
				if(tile[i][j].id == 8){
					tile[i][j].color = Color.BLACK;		
				}
				if(tile[i][j].id == 9){
				}
				if(tile[i][j].id == 10){
					tile[i][j].color = new Color(255, 0, 255);
				}
			}
		}
	}
}




