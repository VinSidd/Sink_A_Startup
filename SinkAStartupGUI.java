import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class SinkAStartupGUI extends JFrame {

    private JButton[][] gridButtons = new JButton[7][7];
    private ArrayList<Startup> startups = new ArrayList<>();
    private GameHelper helper = new GameHelper();
    private JTextArea messageArea;
    private int numOfGuess = 0;

    public SinkAStartupGUI() {
        setTitle("Sink A Startup - GUI Version");
        setSize(600, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel(new GridLayout(7, 7));
        String alpha = "abcdefg";

        // build GUI grid
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                String coord = "" + alpha.charAt(c) + r;
                JButton btn = new JButton(coord);
                btn.setFont(new Font("Arial", Font.BOLD, 14));

                final String selected = coord;

                btn.addActionListener(e -> handleGuess(selected, btn));

                gridButtons[r][c] = btn;
                gridPanel.add(btn);
            }
        }

        // message area
        messageArea = new JTextArea(5, 20);
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        JScrollPane scroll = new JScrollPane(messageArea);

        add(gridPanel, BorderLayout.CENTER);
        add(scroll, BorderLayout.SOUTH);

        setupGame();
        setVisible(true);
    }

    // ------- Setup game -------
    public void setupGame() {
        Startup one = new Startup("poniez");
        Startup two = new Startup("hacqi");
        Startup three = new Startup("cabista");

        startups.add(one);
        startups.add(two);
        startups.add(three);

        for (Startup s : startups) {
            ArrayList<String> loc = helper.placeStartup(3);
            s.setLocationCells(loc);
        }

        messageArea.append("Your goal is to sink 3 startups:\n");
        messageArea.append("poniez, hacqi, cabista\n\n");
        messageArea.append("Click the grid to play.\n\n");
    }

    // ------- Handle Button Click -------
    public void handleGuess(String userGuess, JButton btn) {
        numOfGuess++;
        String result = "miss";

        for (int i = 0; i < startups.size(); i++) {
            result = startups.get(i).checkYourself(userGuess);

            if (result.equals("hit")) {
                btn.setBackground(Color.ORANGE);
                messageArea.append("HIT on " + userGuess + "\n");
                return;
            } else if (result.equals("kill")) {
                btn.setBackground(Color.RED);
                messageArea.append("KILLED a startup!\n");
                startups.remove(i);
                break;
            }
        }

        if (result.equals("miss")) {
            btn.setBackground(Color.GRAY);
            messageArea.append("MISS at " + userGuess + "\n");
        }

        btn.setEnabled(false);

        if (startups.isEmpty()) {
            endGame();
        }
    }

    // ------- End game -------
    public void endGame() {
        messageArea.append("\nAll Startups are dead!\n");

        if (numOfGuess <= 18)
            messageArea.append("You won in " + numOfGuess + " guesses.\n");
        else
            messageArea.append("Too slow! Took " + numOfGuess + " guesses.\n");

        // disable all buttons
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                gridButtons[r][c].setEnabled(false);
            }
        }
    }

    public static void main(String[] args) {
        new SinkAStartupGUI();
    }
}

/* ------------------------------------------------------------
   Startup Class (same as your CLI version but simplified)
--------------------------------------------------------------*/
class Startup {
    private ArrayList<String> locationCells;
    private String name;

    public Startup(String n) {
        name = n;
    }

    public void setLocationCells(ArrayList<String> loc) {
        locationCells = loc;
    }

    public String checkYourself(String userInput) {
        String result = "miss";
        int index = locationCells.indexOf(userInput);

        if (index >= 0) {
            locationCells.remove(index);
            if (locationCells.isEmpty()) {
                result = "kill";
            } else {
                result = "hit";
            }
        }
        return result;
    }
}

/* ------------------------------------------------------------
   GameHelper – same grid logic as your original CLI version
--------------------------------------------------------------*/
class GameHelper {
    private static final String ALPHABET = "abcdefg";
    private static final int GRID_LENGTH = 7;
    private static final int GRID_SIZE = 49;

    private int[] grid = new int[GRID_SIZE];
    private Random random = new Random();
    private int startupCount = 0;

    public ArrayList<String> placeStartup(int startupSize) {
        int[] coords = new int[startupSize];
        boolean success = false;
        int attempts = 0;
        startupCount++;

        int increment = (startupCount % 2 == 0) ? 1 : GRID_LENGTH;

        while (!success && attempts++ < 200) {
            int start = random.nextInt(GRID_SIZE);

            for (int i = 0; i < startupSize; i++) coords[i] = start + i * increment;

            if (startupFits(coords, increment) && coordsAvailable(coords)) {
                success = true;
            }
        }

        for (int i : coords) grid[i] = 1;

        return convertToAlphaCells(coords);
    }

    private boolean startupFits(int[] coords, int increment) {
        int last = coords[coords.length - 1];
        if (increment == 1)
            return (coords[0] / GRID_LENGTH) == (last / GRID_LENGTH);
        else
            return last < GRID_SIZE;
    }

    private boolean coordsAvailable(int[] coords) {
        for (int c : coords)
            if (c >= GRID_SIZE || grid[c] != 0)
                return false;
        return true;
    }

    private ArrayList<String> convertToAlphaCells(int[] coords) {
        ArrayList<String> list = new ArrayList<>();
        for (int index : coords) {
            int row = index / GRID_LENGTH;
            int col = index % GRID_LENGTH;
            list.add("" + ALPHABET.charAt(col) + row);
        }
        return list;
    }
}

