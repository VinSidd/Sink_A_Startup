package startup_project_game;
import java.util.*;
public class Startup {
    private ArrayList<String> locationCells;
    private String name;

    public void setLocationCells(ArrayList<String> loc){
        locationCells = loc;
    }

    public void setName(String n){
        name = n;
    }

    public String checkYourself(String userInput){
        String result = "miss";
        int index = locationCells.indexOf(userInput);
        if (index >= 0){
            locationCells.remove(index);
            if (locationCells.isEmpty()){
                result = "kill";
                System.out.println("Ouch! You sunk " + name + "   :(");

            }else{
                result = "hit";
            }
        }
        return result;
    }
}

class StartupBurst{

    GameHelper helper = new GameHelper();
    @SuppressWarnings({ "unchecked", "rawtypes" })
    ArrayList <Startup> startups = new ArrayList();
    int numOfGuess = 0;

    public void setUpGame (){
        Startup one = new Startup();
        one.setName("poniez");
        Startup two = new Startup();
        two.setName("hacqi");
        Startup three = new Startup();
        three.setName("cabista");
        startups.add(one);
        startups.add(two);
        startups.add(three);
        System.out.println("Yout goal isto sink three startups.");
        System.out.println("poniez, hacqi, cabista");
        System.out.println("Startups are placed either Horizontally or Vertically under the following grid locations:");
        System.out.println();
        System.out.print("a1 a2 a3 a4 a5 a6 a7\n");
        System.out.print("b1 b2 b3 b4 b5 b6 b7\n");
        System.out.print("c1 c2 c3 c4 c5 c6 c7\n");
        System.out.print("d1 d2 d3 d4 d5 d6 d7\n");
        System.out.print("e1 e2 e3 e4 e5 e6 e7\n");
        System.out.print("f1 f2 f3 f4 f5 f6 f7\n");
        System.out.print("g1 g2 g3 g4 g5 g6 g7\n");
        System.out.println();
        System.out.println("Try to sink them all in the fewest number of guesses.");
        for(Startup s : startups){
            ArrayList <String> newLocation = helper.placeStartup(3);
            s.setLocationCells(newLocation);
        }
    }

    public void startPlaying(){
        while (!startups.isEmpty()){
            String userGuess = helper.getUserInput("Enter a guess: ");
            checkUserGuess(userGuess);
        }
        finishGame();
    }

    public void checkUserGuess(String userGuess){
        numOfGuess++;
        String result = "miss";
        for(Startup s : startups){
            result = s.checkYourself(userGuess);
            if(result.equals("hit")){
                break;
            }
            if(result.equals("kill")){
                startups.remove(s);
                break;
            }
        }
        System.out.println(result);
    }

    public void finishGame(){
        System.out.println("All Startups are dead! Your stock is now worthless.");
        if(numOfGuess <= 18){
            System.out.println("It only took you "+numOfGuess + " guesses.");
            System.out.println("You got out before your options sank.");

        }else{
            System.out.println("Took you long enough. "+ numOfGuess + " guesses.");
            System.out.println("Fish are dancing with your options.");
        }
    }

    public static void main(String [] args){
        StartupBurst game = new StartupBurst();
        game.setUpGame();
        game.startPlaying();
    }
}

class GameHelper {
    private static final String ALPHABET = "abcdefg";
    private static final int GRID_LENGTH = 7;
    private static final int GRID_SIZE = 49;
    private static final int MAX_ATTEMPTS = 200;
    static final int HORIZONTAL_INCREMENT = 1;
    static final int VERTICAL_INCREMENT = GRID_LENGTH;
    private final int [] grid = new int [GRID_SIZE];
    private final Random random = new Random();
    private int startupCount = 0;

    @SuppressWarnings("resource")
    public String getUserInput(String prompt){
        System.out.print(prompt + ": ");
        Scanner scanner= new Scanner (System.in);
        return scanner.nextLine().toLowerCase();

    }

    public ArrayList<String> placeStartup (int startupSize){
        int [] startupCoords = new int [startupSize];
        int attempts = 0;
        boolean success = false;
        startupCount++;
        int increment = getIncrement();
        while (!success & attempts++ < MAX_ATTEMPTS){
            int location = random.nextInt(GRID_SIZE);
            for(int i = 0; i < startupCoords.length ;i++){
                startupCoords[i] = location;
                location+= increment;
            }
            //System.out.println("Trying : "+ Arrays.toString(startupCoords));
            if (startupFits(startupCoords,increment)){
                success = coordsAvailable(startupCoords);
            }
        }
        savePositionToGrid(startupCoords);
        ArrayList<String> alphaCells = convertCoordsToAlphaFormat(startupCoords);
        System.out.println("Placed At: "+ alphaCells);
        return alphaCells;

    }

    private boolean startupFits(int[] startupCoords, int increment){ 
        int finalLocation = startupCoords[startupCoords.length-1 ];
        if (increment == HORIZONTAL_INCREMENT){
            return calcRowFromIndex(startupCoords[0]) == calcRowFromIndex(finalLocation);
        }
        else{
            return finalLocation<GRID_SIZE;
        }
    }

    private boolean coordsAvailable(int [] startupCoords){
        for (int coord : startupCoords){
            if(grid[coord] != 0){
            //    System.out.println("position :"+coord+ "aldready taken.");
                return false;
            }
        }
        return true;
    }

    private void savePositionToGrid(int [] startupCoords){
        for(int index : startupCoords){
            grid[index] = 1;
        }
    }

    private ArrayList<String> convertCoordsToAlphaFormat(int[] startupCoords){
        ArrayList <String> alphaCells = new ArrayList<>();
        for(int index :startupCoords){
            String alphaCoords = getAlphaCoordsFromIndex(index);
            alphaCells.add(alphaCoords);
        }
        return alphaCells;
    }

    private String getAlphaCoordsFromIndex(int index){
        int row = calcRowFromIndex(index);
        int column = index % GRID_LENGTH;
        String letter = ALPHABET.substring(column,column+1);
        return letter + row;
    }

    private int calcRowFromIndex(int index){
        return index/GRID_LENGTH;
    }

    private int getIncrement(){
        if(startupCount % 2 == 0){
            return HORIZONTAL_INCREMENT;
        }else{
            return VERTICAL_INCREMENT;
        }
    }
}