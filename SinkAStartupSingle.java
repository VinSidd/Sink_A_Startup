/**
 * SinkAStartupSingle.java
 *
 * Single-file Java Swing game: "Sink A Startup" (Gaming Style)
 * - Combined into one .java file (all classes nested)
 * - Uses PNG assets in assets/ (ships/, explosions/, sound/)
 * - Plays looping background music (WAV)
 * - Camera shake on hit/kill
 * - Particle debris animation
 * - Debug mode to reveal ship icons
 * - Restart button
 *
 * Project folder should contain:
 *  SinkAStartupSingle.java
 *  assets/
 *    ships/ship_poniez.png
 *    ships/ship_hacqi.png
 *    ships/ship_cabista.png
 *    explosions/explosion1.png
 *    explosions/explosion2.png
 *    explosions/explosion3.png
 *    sound/background_music.wav
 *    sound/hit.wav
 *    sound/kill.wav
 *
 * If assets are absent, the code gracefully draws fallback icons and uses simple beeps.
 *
 * Java: any standard JDK with Swing (11+ recommended)
 *
 * Author: ChatGPT (GPT-5 Thinking mini)
 * Date: 2025-12-11
 */

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class SinkAStartupSingle extends JFrame {

    // ------------ Config ------------
    private static final int GRID_LEN = 7;
    private static final String ALPH = "abcdefg";
    private static final String ASSET_ROOT = "assets"; // place assets/ next to this file
    // ---------------------------------

    // UI & Game state
    private final JButton[][] gridButtons = new JButton[GRID_LEN][GRID_LEN];
    private final GameLogic logic = new GameLogic();
    private final ParticlePanel particlePanel = new ParticlePanel();
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Welcome, Captain. Sink the startups!");
    private final JCheckBox debugCheck = new JCheckBox("Show startup positions (Debug)");
    private Clip bgMusicClip;
    private int guesses = 0;

    // Assets (may be null if not found)
    private BufferedImage shipPoniez, shipHacqi, shipCabista;
    private BufferedImage[] explosionFrames;
    private File soundHitFile, soundKillFile, musicFile;

    // Constructor
    public SinkAStartupSingle() {
        super("Sink A Startup — All-in-One");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(new Color(18, 20, 24));

        loadAssets();
        buildUI();
        startBackgroundMusic();
        restartGame(false);
    }

    // ---------- UI Build ----------
    private void buildUI() {
        // Top bar
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        statusLabel.setForeground(Color.CYAN);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        top.add(statusLabel, BorderLayout.CENTER);

        JButton restartBtn = new JButton("Restart");
        restartBtn.addActionListener(e -> restartGame(true));
        top.add(restartBtn, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Center: layered grid + particles
        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(720, 720));
        JPanel gridPanel = new JPanel(new GridLayout(GRID_LEN, GRID_LEN, 6, 6));
        gridPanel.setOpaque(false);
        gridPanel.setBounds(10, 10, 700, 700);

        Font gf = new Font("Verdana", Font.BOLD, 14);
        for (int r = 0; r < GRID_LEN; r++) {
            for (int c = 0; c < GRID_LEN; c++) {
                JButton b = new JButton(ALPH.charAt(c) + "" + r);
                b.setFont(gf);
                b.setFocusPainted(false);
                b.setContentAreaFilled(false);
                b.setOpaque(false);
                b.setBorder(new CompoundBorder(new GlowBorder(new Color(18,138,255),2), new RoundedBorder(8)));
                final int rr = r, cc = c;
                b.addActionListener(e -> handleClick(rr, cc, b));
                gridButtons[r][c] = b;
                JPanel wrap = new JPanel(new BorderLayout());
                wrap.setOpaque(false);
                wrap.setBorder(new EmptyBorder(6,6,6,6));
                wrap.add(b, BorderLayout.CENTER);
                gridPanel.add(wrap);
            }
        }

        particlePanel.setBounds(10, 10, 700, 700);
        particlePanel.setOpaque(false);
        layered.add(gridPanel, Integer.valueOf(0));
        layered.add(particlePanel, Integer.valueOf(1));
        add(layered, BorderLayout.CENTER);

        // Right: controls + log
        JPanel right = new JPanel(new BorderLayout(8,8));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(300, 0));

        JPanel ctrl = new JPanel(new GridLayout(0,1,6,6));
        ctrl.setOpaque(false);

        // Difficulty selector
        JLabel diffLabel = new JLabel("Difficulty:");
        diffLabel.setForeground(Color.LIGHT_GRAY);
        ctrl.add(diffLabel);
        String[] diffs = {"Easy (3 startups size3)", "Medium (4 startups size3)", "Hard (3 startups size4)"};
        JComboBox<String> diffCombo = new JComboBox<>(diffs);
        diffCombo.setSelectedIndex(1);
        diffCombo.addActionListener(e -> {
            int idx = diffCombo.getSelectedIndex();
            if (idx==0) logic.setDifficulty(GameLogic.Difficulty.EASY);
            else if (idx==1) logic.setDifficulty(GameLogic.Difficulty.MEDIUM);
            else logic.setDifficulty(GameLogic.Difficulty.HARD);
            restartGame(true);
        });
        ctrl.add(diffCombo);

        // Debug toggle
        debugCheck.setForeground(Color.WHITE);
        debugCheck.setOpaque(false);
        debugCheck.addActionListener(e -> revealDebug());
        ctrl.add(debugCheck);

        // Sound toggle
        JCheckBox soundToggle = new JCheckBox("Enable music");
        soundToggle.setSelected(true);
        soundToggle.setForeground(Color.WHITE);
        soundToggle.setOpaque(false);
        soundToggle.addActionListener(e -> {
            if (soundToggle.isSelected()) startBackgroundMusic();
            else stopBackgroundMusic();
        });
        ctrl.add(soundToggle);

        right.add(ctrl, BorderLayout.NORTH);

        // Log area
        logArea.setEditable(false);
        logArea.setBackground(new Color(24,28,34));
        logArea.setForeground(Color.WHITE);
        logArea.setBorder(new CompoundBorder(new MatteBorder(2,2,2,2,new Color(18,138,255)), new EmptyBorder(6,6,6,6)));
        JScrollPane sp = new JScrollPane(logArea);
        sp.setPreferredSize(new Dimension(260, 520));
        right.add(sp, BorderLayout.CENTER);

        add(right, BorderLayout.EAST);

        // Bottom help
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        JLabel help = new JLabel("Legend: Orange=Hit  Red=Kill  Gray=Miss  |  Click tiles to attack.");
        help.setForeground(Color.WHITE);
        bottom.add(help);
        add(bottom, BorderLayout.SOUTH);

        applyTheme();
    }

    // ---------- Theme ----------
    private void applyTheme() {
        getContentPane().setBackground(new Color(17,20,27));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        repaint();
    }

    // ---------- Asset Loading ----------
    private void loadAssets() {
        try {
            shipPoniez = ImageIO.read(new File(ASSET_ROOT + "/ships/ship_poniez.png"));
        } catch (Exception e) { shipPoniez = null; }
        try {
            shipHacqi = ImageIO.read(new File(ASSET_ROOT + "/ships/ship_hacqi.png"));
        } catch (Exception e) { shipHacqi = null; }
        try {
            shipCabista = ImageIO.read(new File(ASSET_ROOT + "/ships/ship_cabista.png"));
        } catch (Exception e) { shipCabista = null; }

        // explosions
        java.util.List<BufferedImage> ex = new ArrayList<>();
        for (int i=1;i<=3;i++){
            try {
                ex.add(ImageIO.read(new File(ASSET_ROOT + "/explosions/explosion"+i+".png")));
            } catch (Exception ignored) {}
        }
        explosionFrames = ex.isEmpty() ? null : ex.toArray(new BufferedImage[0]);

        // sounds
        soundHitFile = new File(ASSET_ROOT + "/sound/hit.wav");
        soundKillFile = new File(ASSET_ROOT + "/sound/kill.wav");
        musicFile = new File(ASSET_ROOT + "/sound/background_music.wav");
    }

    // ---------- Music ----------
    private void startBackgroundMusic() {
        if (musicFile == null || !musicFile.exists()) return;
        stopBackgroundMusic();
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(musicFile);
            bgMusicClip = AudioSystem.getClip();
            bgMusicClip.open(ais);
            bgMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            System.out.println("Could not start background music: " + e);
        }
    }
    private void stopBackgroundMusic() {
        if (bgMusicClip != null && bgMusicClip.isRunning()) {
            bgMusicClip.stop();
            bgMusicClip.close();
            bgMusicClip = null;
        }
    }

    // ---------- Game actions ----------
    private void restartGame(boolean quiet) {
        guesses = 0;
        logic.reset();
        logic.placeStartups();
        // UI reset
        for (int r=0;r<GRID_LEN;r++){
            for (int c=0;c<GRID_LEN;c++){
                JButton b = gridButtons[r][c];
                b.setEnabled(true);
                b.setBackground(null);
                b.setIcon(null);
                b.setText(ALPH.charAt(c) + "" + r);
            }
        }
        particlePanel.clear();
        logArea.setText("");
        if (!quiet) appendLog("New mission: Sink all startups!");
        revealDebug();
    }

    private void appendLog(String s){
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void revealDebug() {
        boolean dbg = debugCheck.isSelected();
        for (int r=0;r<GRID_LEN;r++){
            for (int c=0;c<GRID_LEN;c++){
                JButton b = gridButtons[r][c];
                if (dbg && logic.isStartupCell(r,c)) {
                    BufferedImage img = chooseShipForCell(r,c);
                    if (img!=null) {
                        b.setIcon(new ImageIcon(img.getScaledInstance(36, 28, Image.SCALE_SMOOTH)));
                        b.setHorizontalTextPosition(SwingConstants.CENTER);
                        b.setVerticalTextPosition(SwingConstants.BOTTOM);
                        b.setText("");
                    } else {
                        b.setText(ALPH.charAt(c) + "" + r);
                    }
                } else {
                    b.setIcon(null);
                    b.setText(ALPH.charAt(c) + "" + r);
                }
            }
        }
    }

    // choose which ship image to show for a position (simple deterministic mapping)
    private BufferedImage chooseShipForCell(int r, int c){
        int idx = (r*GRID_LEN + c) % 3;
        if (idx == 0) return shipPoniez;
        if (idx == 1) return shipHacqi;
        return shipCabista;
    }

    // Handle click on grid cell
    private void handleClick(int r, int c, JButton btn) {
        if (!btn.isEnabled()) return;
        guesses++;
        String result = logic.checkGuess(r, c);

        switch(result) {
            case "miss":
                markMiss(btn);
                appendLog("MISS at " + ALPH.charAt(c) + r);
                playSound(soundHitFile);
                break;
            case "hit":
                markHit(btn, r, c);
                appendLog("HIT at " + ALPH.charAt(c) + r);
                particlePanel.spawnParticles((r*700/GRID_LEN)+(700/GRID_LEN/2), (c*700/GRID_LEN)+(700/GRID_LEN/2));
                cameraShake(8);
                playSound(soundHitFile);
                break;
            case "kill":
                markKill(btn, r, c);
                appendLog("KILL! Startup sunk at " + ALPH.charAt(c) + r);
                particlePanel.spawnParticles((r*700/GRID_LEN)+(700/GRID_LEN/2), (c*700/GRID_LEN)+(700/GRID_LEN/2), 40);
                cameraShake(18);
                playSound(soundKillFile);
                break;
            case "win":
                appendLog("ALL STARTUPS SUNK in " + guesses + " guesses!");
                markKill(btn, r, c);
                playSound(soundKillFile);
                cameraShake(24);
                disableAllGrid();
                break;
        }
        btn.setEnabled(false);
    }

    private void markMiss(JButton b) {
        b.setBackground(new Color(120,120,120,200));
        b.setOpaque(true);
        b.setText("");
    }
    private void markHit(JButton b, int r, int c) {
        b.setBackground(new Color(255,140,0,220));
        b.setOpaque(true);
        setExplosionIconOnButton(b);
        b.setText("");
    }
    private void markKill(JButton b, int r, int c) {
        b.setBackground(new Color(200,35,35,220));
        b.setOpaque(true);
        setExplosionIconOnButton(b);
        b.setText("");
    }
    private void setExplosionIconOnButton(JButton b) {
        if (explosionFrames != null && explosionFrames.length>0) {
            b.setIcon(new ImageIcon(explosionFrames[0].getScaledInstance(48,48,Image.SCALE_SMOOTH)));
            // animate small frame swap
            new Thread(() -> {
                try {
                    for (int f=0; f<explosionFrames.length; f++) {
                        Image ic = explosionFrames[f].getScaledInstance(48,48,Image.SCALE_SMOOTH);
                        SwingUtilities.invokeLater(() -> b.setIcon(new ImageIcon(ic)));
                        Thread.sleep(90);
                    }
                } catch (InterruptedException ignored) {}
            }).start();
        } else {
            b.setIcon(null);
        }
    }

    private void disableAllGrid() {
        for (int r=0;r<GRID_LEN;r++) for (int c=0;c<GRID_LEN;c++) gridButtons[r][c].setEnabled(false);
    }

    // Camera shake of the main frame
    private void cameraShake(int intensity) {
        final Point orig = getLocation();
        new Thread(() -> {
            try {
                for (int i=0;i<8;i++){
                    int dx = (int)(Math.random()*intensity*2 - intensity);
                    int dy = (int)(Math.random()*intensity*2 - intensity);
                    setLocation(orig.x + dx, orig.y + dy);
                    Thread.sleep(35);
                }
            } catch (Exception ignored) {}
            finally {
                setLocation(orig);
            }
        }).start();
    }

    // Play short wav sound if available
    private void playSound(File wav) {
        if (wav==null || !wav.exists()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(wav)) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
            } catch (Exception e) {
                Toolkit.getDefaultToolkit().beep();
            }
        }).start();
    }

    // ---------- Inner: GameLogic ----------
    private static class GameLogic {
        enum Difficulty { EASY, MEDIUM, HARD }
        private Difficulty difficulty = Difficulty.MEDIUM;
        private final int GRID = GRID_LEN;
        // Startup lists stored as sets of positions encoded (r*GRID + c)
        private final java.util.List<Set<Integer>> startups = new ArrayList<>();
        private final Random rnd = new Random();

        GameLogic() {
            // default
            setDifficulty(Difficulty.MEDIUM);
            placeStartups();
        }

        void setDifficulty(Difficulty d) { difficulty = d; }
        void setDifficultyEnum(Difficulty d) { difficulty = d; }

        void reset() {
            startups.clear();
        }

        void placeStartups() {
            startups.clear();
            int count, size;
            switch (difficulty) {
                case EASY: count = 3; size = 3; break;
                case MEDIUM: count = 4; size = 3; break;
                case HARD: count = 3; size = 4; break;
                default: count = 3; size = 3;
            }
            int attempts = 0;
            while (startups.size() < count && attempts++ < 2000) {
                boolean vertical = rnd.nextBoolean();
                int r = rnd.nextInt(GRID);
                int c = rnd.nextInt(GRID);
                // ensure fits
                int endR = vertical ? r + size - 1 : r;
                int endC = vertical ? c : c + size - 1;
                if (endR >= GRID || endC >= GRID) continue;
                Set<Integer> pos = new HashSet<>();
                boolean overlap = false;
                for (int k=0;k<size;k++) {
                    int rr = vertical ? r+k : r;
                    int cc = vertical ? c : c+k;
                    int code = rr*GRID + cc;
                    for (Set<Integer> s : startups) if (s.contains(code)) { overlap = true; break; }
                    if (overlap) break;
                    pos.add(code);
                }
                if (!overlap) startups.add(pos);
            }
            // fallback if something went wrong: fill random singles
            while (startups.size() < count) {
                Set<Integer> pos = new HashSet<>();
                pos.add(rnd.nextInt(GRID*GRID));
                startups.add(pos);
            }
        }

        /**
         * Check a guess: returns "miss", "hit", "kill", or "win".
         */
        String checkGuess(int r, int c) {
            int code = r*GRID + c;
            for (Iterator<Set<Integer>> it = startups.iterator(); it.hasNext();) {
                Set<Integer> s = it.next();
                if (s.contains(code)) {
                    s.remove(code);
                    if (s.isEmpty()) {
                        it.remove();
                        if (startups.isEmpty()) return "win";
                        return "kill";
                    } else {
                        return "hit";
                    }
                }
            }
            return "miss";
        }

        boolean isStartupCell(int r, int c) {
            int code = r*GRID + c;
            for (Set<Integer> s : startups) if (s.contains(code)) return true;
            return false;
        }
    }

    // ---------- Inner: ParticlePanel ----------
    private class ParticlePanel extends JPanel {
        private final java.util.List<Particle> particles = Collections.synchronizedList(new ArrayList<>());
        private final Timer timer;

        ParticlePanel() {
            setOpaque(false);
            timer = new Timer(25, e -> {
                synchronized (particles) {
                    Iterator<Particle> it = particles.iterator();
                    while (it.hasNext()) {
                        Particle p = it.next();
                        p.update();
                        if (!p.alive) it.remove();
                    }
                }
                repaint();
            });
            timer.start();
        }

        void spawnParticles(int cx, int cy) { spawnParticles(cx, cy, 20); }
        void spawnParticles(int cx, int cy, int count) {
            synchronized (particles) {
                for (int i=0;i<count;i++) particles.add(new Particle(cx + (int)(Math.random()*40-20), cy + (int)(Math.random()*40-20)));
            }
        }
        void clear() { synchronized (particles) { particles.clear(); } }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            for (Particle p : particles) p.draw(g2);
            g2.dispose();
        }
    }

    private static class Particle {
        int x,y;
        double dx,dy;
        int life = 25;
        boolean alive = true;
        Color color;

        Particle(int x, int y) {
            this.x = x; this.y = y;
            double ang = Math.random()*Math.PI*2;
            double sp = 2 + Math.random()*4;
            dx = Math.cos(ang)*sp;
            dy = Math.sin(ang)*sp;
            color = new Color(200 + (int)(Math.random()*55), 80 + (int)(Math.random()*120), 20);
        }
        void update() {
            x += dx; y += dy;
            dy += 0.12; // gravity-ish
            life--;
            if (life <= 0) alive = false;
        }
        void draw(Graphics2D g2) {
            float alpha = Math.max(0f, life / 25f);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(color);
            g2.fillOval(x-4, y-4, 8, 8);
        }
    }

    // ---------- UI helper borders ----------
    private static class RoundedBorder implements Border {
        private int radius;
        RoundedBorder(int r){ radius=r; }
        public Insets getBorderInsets(Component c) { return new Insets(radius,radius,radius,radius); }
        public boolean isBorderOpaque() { return false; }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(120,120,120,80));
            g2.drawRoundRect(x+1,y+1,w-3,h-3,radius,radius);
            g2.dispose();
        }
    }
    private static class GlowBorder implements Border {
        private Color glow;
        private int thickness;
        GlowBorder(Color g, int t){ glow=g; thickness=t; }
        public Insets getBorderInsets(Component c){ return new Insets(thickness,thickness,thickness,thickness); }
        public boolean isBorderOpaque(){ return false; }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h){
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setColor(glow);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            for(int i=0;i<thickness;i++) g2.drawRoundRect(x+i,y+i,w-1-2*i,h-1-2*i,10,10);
            g2.dispose();
        }
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SinkAStartupSingle app = new SinkAStartupSingle();
            app.setVisible(true);
        });
    }
}
