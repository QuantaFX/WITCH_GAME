import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class LevelBuilder extends JFrame {
    private DrawingPanel drawingPanel;
    private int levelCounter = 1;
    private JComboBox<String> elementTypeComboBox;
    private static final String PLATFORM = "Platform";
    private static final String ENEMY = "Enemy";
    private static final String PLAYER = "Player";
    private static final String DOOR = "Door";
    
    // Track the currently loaded level file
    private File currentLevelFile = null;
    
    public LevelBuilder() {
        setTitle("Level Builder");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        drawingPanel = new DrawingPanel();
        
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        // Element type selection
        JPanel typePanel = new JPanel();
        JLabel typeLabel = new JLabel("Element Type:");
        elementTypeComboBox = new JComboBox<>(new String[]{PLATFORM, ENEMY, PLAYER, DOOR});
        elementTypeComboBox.addActionListener(e -> drawingPanel.setCurrentElementType((String)elementTypeComboBox.getSelectedItem()));
        
        typePanel.add(typeLabel);
        typePanel.add(elementTypeComboBox);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        
        JButton addButton = new JButton("Add Element");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawingPanel.addElement();
            }
        });

        JButton removeButton = new JButton("Remove Element");
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawingPanel.removeLastElement();
            }
        });
        
        JButton clearButton = new JButton("Clear All");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawingPanel.clearAll();
            }
        });
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        });
        
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadFromFile();
            }
        });
        
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        
        controlPanel.add(typePanel, BorderLayout.NORTH);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(drawingPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
      
        setVisible(true);
    }
    
    private void saveToFile() {
        try {
            File fileToSave;
            
            // If we have a currently loaded level, save to that file
            if (currentLevelFile != null) {
                fileToSave = currentLevelFile;
            } else {
                // Find the highest level number and increment
                int highestLevel = findHighestLevelNumber();
                // Save to the correct relative path
                fileToSave = new File("../../lvl_" + (highestLevel + 1) + ".level");
            }
            
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            
            // Root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("level");
            doc.appendChild(rootElement);
            
            // Add platforms
            Element platformsElement = doc.createElement("platforms");
            rootElement.appendChild(platformsElement);
            
            for (GameElement element : drawingPanel.getElements()) {
                if (element.getType().equals(PLATFORM)) {
                    Element platformElement = doc.createElement("platform");
                    platformsElement.appendChild(platformElement);
                    
                    platformElement.setAttribute("x", Integer.toString(element.x));
                    platformElement.setAttribute("y", Integer.toString(element.y));
                    platformElement.setAttribute("width", Integer.toString(element.width));
                    platformElement.setAttribute("height", Integer.toString(element.height));
                }
            }
            
            // Add enemies
            Element enemiesElement = doc.createElement("enemies");
            rootElement.appendChild(enemiesElement);
            
            for (GameElement element : drawingPanel.getElements()) {
                if (element.getType().equals(ENEMY)) {
                    Element enemyElement = doc.createElement("enemy");
                    enemiesElement.appendChild(enemyElement);
                    
                    enemyElement.setAttribute("x", Integer.toString(element.x));
                    enemyElement.setAttribute("y", Integer.toString(element.y));
                }
            }
            
            // Add player (if exists)
            GameElement playerElement = drawingPanel.getPlayerElement();
            if (playerElement != null) {
                Element playerXml = doc.createElement("player");
                rootElement.appendChild(playerXml);
                
                playerXml.setAttribute("x", Integer.toString(playerElement.x));
                playerXml.setAttribute("y", Integer.toString(playerElement.y));
            }
            
            // Add door (if exists)
            GameElement doorElement = drawingPanel.getDoorElement();
            if (doorElement != null) {
                Element doorXml = doc.createElement("door");
                rootElement.appendChild(doorXml);
                
                doorXml.setAttribute("x", Integer.toString(doorElement.x));
                doorXml.setAttribute("y", Integer.toString(doorElement.y));
                doorXml.setAttribute("width", Integer.toString(doorElement.width));
                doorXml.setAttribute("height", Integer.toString(doorElement.height));
            }
            
            // Write to XML file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(fileToSave);
            transformer.transform(source, result);
            
            // Store the current level file name
            currentLevelFile = fileToSave;
            
            JOptionPane.showMessageDialog(this, "Saved level to " + fileToSave.getPath());
        } catch (ParserConfigurationException | TransformerException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(),
                                         "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private int findHighestLevelNumber() {
        int highest = 0;
        // Check in the project root directory (../../)
        File dir = new File("../../");
        File[] files = dir.listFiles((d, name) -> name.matches("lvl_\\d+\\.level"));
        
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                try {
                    // Extract the number part
                    int num = Integer.parseInt(name.substring(4, name.length() - 6));
                    highest = Math.max(highest, num);
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    // Skip files with invalid format
                }
            }
        }
        
        return highest;
    }
    
    private void loadFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("../../"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".level");
            }
            
            public String getDescription() {
                return "Level Files (*.level)";
            }
        });
        
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(selectedFile);
                doc.getDocumentElement().normalize();
                
                ArrayList<GameElement> loadedElements = new ArrayList<>();
                GameElement playerElement = null;
                GameElement doorElement = null;
                
                // Load platforms from new format
                NodeList platformNodes = doc.getElementsByTagName("platform");
                for (int i = 0; i < platformNodes.getLength(); i++) {
                    Node node = platformNodes.item(i);
                    
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        
                        try {
                            int x = Integer.parseInt(element.getAttribute("x"));
                            int y = Integer.parseInt(element.getAttribute("y"));
                            int width = Integer.parseInt(element.getAttribute("width"));
                            int height = Integer.parseInt(element.getAttribute("height"));
                            
                            loadedElements.add(new GameElement(x, y, width, height, PLATFORM));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number format in platform element");
                        }
                    }
                }
                
                // Handle old format (rectangle elements directly under level)
                if (platformNodes.getLength() == 0) {
                    NodeList rectangleNodes = doc.getElementsByTagName("rectangle");
                    for (int i = 0; i < rectangleNodes.getLength(); i++) {
                        Node node = rectangleNodes.item(i);
                        
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element element = (Element) node;
                            
                            try {
                                int x = Integer.parseInt(element.getAttribute("x"));
                                int y = Integer.parseInt(element.getAttribute("y"));
                                int width = Integer.parseInt(element.getAttribute("width"));
                                int height = Integer.parseInt(element.getAttribute("height"));
                                
                                loadedElements.add(new GameElement(x, y, width, height, PLATFORM));
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid number format in rectangle element");
                            }
                        }
                    }
                }
                
                // Load enemies
                NodeList enemyNodes = doc.getElementsByTagName("enemy");
                for (int i = 0; i < enemyNodes.getLength(); i++) {
                    Node node = enemyNodes.item(i);
                    
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        
                        try {
                            int x = Integer.parseInt(element.getAttribute("x"));
                            int y = Integer.parseInt(element.getAttribute("y"));
                            
                            loadedElements.add(new GameElement(x, y, 20, 20, ENEMY));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number format in enemy element");
                        }
                    }
                }
                
                // Load player
                NodeList playerNodes = doc.getElementsByTagName("player");
                if (playerNodes.getLength() > 0) {
                    Node node = playerNodes.item(0);
                    
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        
                        try {
                            int x = Integer.parseInt(element.getAttribute("x"));
                            int y = Integer.parseInt(element.getAttribute("y"));
                            
                            playerElement = new GameElement(x, y, 20, 30, PLAYER);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number format in player element");
                        }
                    }
                }
                
                // Load door
                NodeList doorNodes = doc.getElementsByTagName("door");
                if (doorNodes.getLength() > 0) {
                    Node node = doorNodes.item(0);
                    
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        
                        try {
                            int x = Integer.parseInt(element.getAttribute("x"));
                            int y = Integer.parseInt(element.getAttribute("y"));
                            int width = Integer.parseInt(element.getAttribute("width"));
                            int height = Integer.parseInt(element.getAttribute("height"));
                            
                            doorElement = new GameElement(x, y, width, height, DOOR);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number format in door element");
                        }
                    }
                }
                
                drawingPanel.setElements(loadedElements, playerElement, doorElement);
                JOptionPane.showMessageDialog(this, "Loaded level from " + selectedFile.getPath());
                
                // Store the current level file
                currentLevelFile = selectedFile;
                
            } catch (ParserConfigurationException | SAXException | IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage(),
                                             "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public static void main(String[] args) {
    	new LevelBuilder();
    }
}

class GameElement {
    int x, y;           
    int width, height;
    private String type;

    public GameElement(int x, int y, int width, int height, String type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
}

class DrawingPanel extends JPanel {
    private ArrayList<GameElement> elements = new ArrayList<>();
    private GameElement selectedElement = null;
    private GameElement playerElement = null;
    private GameElement doorElement = null;
    
    private int lastMouseX;
    private int lastMouseY;
   
    private boolean resizing = false;
    private String currentElementType = "Platform";
    
    public DrawingPanel() {
        setBackground(Color.WHITE);
        MyMouseListener mouseListener = new MyMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
    }
    
    public ArrayList<GameElement> getElements() {
        return elements;
    }
    
    public GameElement getPlayerElement() {
        return playerElement;
    }
    
    public GameElement getDoorElement() {
        return doorElement;
    }
    
    public void setCurrentElementType(String type) {
        currentElementType = type;
    }
    
    public void setElements(ArrayList<GameElement> elements, GameElement player, GameElement door) {
        this.elements = elements;
        this.playerElement = player;
        this.doorElement = door;
        selectedElement = null;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw platforms
        for (GameElement element : elements) {
            if (element.getType().equals("Platform")) {
                g.setColor(Color.GRAY);
                g.fillRect(element.x, element.y, element.width, element.height);
                g.setColor(Color.BLACK);
                g.drawRect(element.x, element.y, element.width, element.height);
            } else if (element.getType().equals("Enemy")) {
                g.setColor(Color.RED);
                g.fillOval(element.x - 10, element.y - 10, 20, 20);
                g.setColor(Color.BLACK);
                g.drawOval(element.x - 10, element.y - 10, 20, 20);
            }
            
            if (element == selectedElement) {
                g.setColor(Color.WHITE);
                g.fillRect(element.x + element.width - 10, element.y + element.height - 10, 10, 10);
                g.setColor(Color.BLACK);
                g.drawRect(element.x + element.width - 10, element.y + element.height - 10, 10, 10);
            }
        }
        
        // Draw player if exists
        if (playerElement != null) {
            g.setColor(Color.BLUE);
            g.fillRect(playerElement.x - 10, playerElement.y - 15, 20, 30);
            g.setColor(Color.BLACK);
            g.drawRect(playerElement.x - 10, playerElement.y - 15, 20, 30);
            
            if (playerElement == selectedElement) {
                g.setColor(Color.WHITE);
                g.fillRect(playerElement.x + 5, playerElement.y + 10, 5, 5);
                g.setColor(Color.BLACK);
                g.drawRect(playerElement.x + 5, playerElement.y + 10, 5, 5);
            }
        }
        
        // Draw door if exists
        if (doorElement != null) {
            g.setColor(Color.GREEN);
            g.fillRect(doorElement.x, doorElement.y, doorElement.width, doorElement.height);
            g.setColor(Color.BLACK);
            g.drawRect(doorElement.x, doorElement.y, doorElement.width, doorElement.height);
            
            if (doorElement == selectedElement) {
                g.setColor(Color.WHITE);
                g.fillRect(doorElement.x + doorElement.width - 10, doorElement.y + doorElement.height - 10, 10, 10);
                g.setColor(Color.BLACK);
                g.drawRect(doorElement.x + doorElement.width - 10, doorElement.y + doorElement.height - 10, 10, 10);
            }
        }
    }
    
    private class MyMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            int mouseX = e.getX();
            int mouseY = e.getY();

            selectedElement = null;
            
            // Check if door is selected
            if (doorElement != null) {
                if (mouseX >= doorElement.x + doorElement.width - 10 && 
                    mouseX <= doorElement.x + doorElement.width &&
                    mouseY >= doorElement.y + doorElement.height - 10 && 
                    mouseY <= doorElement.y + doorElement.height) {
                    
                    selectedElement = doorElement;
                    resizing = true;
                } else if (mouseX >= doorElement.x && mouseX <= doorElement.x + doorElement.width &&
                    mouseY >= doorElement.y && mouseY <= doorElement.y + doorElement.height) {
                    
                    selectedElement = doorElement;
                    resizing = false;
                }
            }
            
            // Check if player is selected
            if (selectedElement == null && playerElement != null) {
                if (mouseX >= playerElement.x - 10 && mouseX <= playerElement.x + 10 &&
                    mouseY >= playerElement.y - 15 && mouseY <= playerElement.y + 15) {
                    
                    selectedElement = playerElement;
                    resizing = false;
                }
            }
            
            // Check if any element is selected
            if (selectedElement == null) {
                for (int i = elements.size() - 1; i >= 0; i--) {
                    GameElement element = elements.get(i);
                    
                    if (element.getType().equals("Platform")) {
                        if (mouseX >= element.x + element.width - 10 && 
                            mouseX <= element.x + element.width &&
                            mouseY >= element.y + element.height - 10 && 
                            mouseY <= element.y + element.height) {
                            
                            selectedElement = element;
                            resizing = true;
                            break;
                        }
    
                        if (mouseX >= element.x && mouseX <= element.x + element.width &&
                            mouseY >= element.y && mouseY <= element.y + element.height) {
                            
                            selectedElement = element;
                            resizing = false;
                            break;
                        }
                    } else if (element.getType().equals("Enemy")) {
                        if (Math.sqrt(Math.pow(mouseX - element.x, 2) + Math.pow(mouseY - element.y, 2)) <= 10) {
                            selectedElement = element;
                            resizing = false;
                            break;
                        }
                    }
                }
            }
            
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedElement == null) {
                return;
            }
            
            int mouseX = e.getX();
            int mouseY = e.getY();
            int deltaX = mouseX - lastMouseX;
            int deltaY = mouseY - lastMouseY;
            
            if (resizing && (selectedElement.getType().equals("Platform") || selectedElement.getType().equals("Door"))) {
                selectedElement.width += deltaX;
                selectedElement.height += deltaY;
                
                if (selectedElement.width < 20) selectedElement.width = 20;
                if (selectedElement.height < 20) selectedElement.height = 20;
            } else {
                if (selectedElement.getType().equals("Enemy") || selectedElement.getType().equals("Player")) {
                    selectedElement.x += deltaX;
                    selectedElement.y += deltaY;
                } else {
                    selectedElement.x += deltaX;
                    selectedElement.y += deltaY;
                }
            }
          
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            
            repaint();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            resizing = false;
        }
    }
    
    public void addElement() {
        GameElement newElement = null;
        
        switch (currentElementType) {
            case "Platform":
                newElement = new GameElement(400, 300, 80, 50, currentElementType);
                elements.add(newElement);
                break;
                
            case "Enemy":
                newElement = new GameElement(400, 300, 20, 20, currentElementType);
                elements.add(newElement);
                break;
                
            case "Player":
                if (playerElement == null) {
                    playerElement = new GameElement(400, 300, 20, 30, currentElementType);
                    newElement = playerElement;
                } else {
                    JOptionPane.showMessageDialog(this, "Only one player can be added to the level");
                }
                break;
                
            case "Door":
                if (doorElement == null) {
                    doorElement = new GameElement(400, 300, 40, 60, currentElementType);
                    newElement = doorElement;
                } else {
                    JOptionPane.showMessageDialog(this, "Only one door can be added to the level");
                }
                break;
        }
        
        selectedElement = newElement;
        repaint();
    }
    
    public void removeLastElement() {
        if (!elements.isEmpty()) {
            if (selectedElement == elements.get(elements.size() - 1)) {
                selectedElement = null;
            }
            elements.remove(elements.size() - 1);
            repaint();
        } else if (doorElement != null && selectedElement == doorElement) {
            doorElement = null;
            selectedElement = null;
            repaint();
        } else if (playerElement != null && selectedElement == playerElement) {
            playerElement = null;
            selectedElement = null;
            repaint();
        }
    }

    public void clearAll() {
        elements.clear();
        playerElement = null;
        doorElement = null;
        selectedElement = null;
        repaint();
    }
}