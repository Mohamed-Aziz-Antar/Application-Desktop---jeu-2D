package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import javax.imageio.ImageIO;
import main.gamePanel;
import main.keyHandler;

public class Player extends entity {
    gamePanel gp;
    keyHandler keyH;
    public final int screenX;
    public final int screenY;
    int hasKey = 0;

    public Player(gamePanel gp, keyHandler keyH) {
        this.gp = gp;
        this.keyH = keyH;
        screenX = gp.screenWidth / 2 - 16;
        screenY = gp.screenHeight / 2 - 23;
        solidArea = new Rectangle();
        solidArea.x = 4;
        solidArea.y = 30;
        solidAreadDefaultX = solidArea.x;
        solidAreadDefaultY = solidArea.y;

        solidArea.width = 18;
        solidArea.height = 13;
        
        setDefaultValues();
        getPlayerImage();
    }
    
    public void getPlayerImage() {
        try {
            up1 = ImageIO.read(getClass().getResourceAsStream("/player/walkUp1-02.png"));
            up2 = ImageIO.read(getClass().getResourceAsStream("/player/walkUp2-02.png"));
            right1 = ImageIO.read(getClass().getResourceAsStream("/player/idleRight-02.png"));
            right2 = ImageIO.read(getClass().getResourceAsStream("/player/walkRight-02.png"));
            left1 = ImageIO.read(getClass().getResourceAsStream("/player/idleLeft.png"));
            left2 = ImageIO.read(getClass().getResourceAsStream("/player/walkLeft-02.png"));
            down1 = ImageIO.read(getClass().getResourceAsStream("/player/walk_without_wepons_down_1-02.png"));
            down2 = ImageIO.read(getClass().getResourceAsStream("/player/walk_without_wepons_down_2-02.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultValues() {
        worldX = gp.tileSize * 23;
        worldY = gp.tileSize * 21;
        speed = 4;
        direction = "down";
    }

    public void update() {
        if (keyH.upPressed == true || keyH.downPressed == true || 
            keyH.leftPressed == true || keyH.rightPressed == true) {
            
            if (keyH.upPressed == true) {
                direction = "up";
            }
            if (keyH.downPressed == true) {
                direction = "down";
            }
            if (keyH.leftPressed == true) {
                direction = "left";
            }
            if (keyH.rightPressed == true) {
                direction = "right";
            }
            
            // CHECK TILE COLLISION
            collisionOn = false;
            gp.cChecker.chekTile(this);
            
            // CHECK OBJECT COLLISION
            int objectIndex = gp.cChecker.checkObject(this, true);
            pickUpObject(objectIndex);
            
            // IF COLLISION IS FALSE, PLAYER CAN MOVE 
            if (collisionOn == false) {
                switch (direction) {
                    case "up":
                        worldY -= speed;
                        break;
                    case "down":
                        worldY += speed;
                        break;
                    case "left":
                        worldX -= speed;
                        break;
                    case "right":
                        worldX += speed;
                        break;
                }
            }
            
            spriteCounter++;
            if (spriteCounter > 12) {
                if (spriteNum == 1) {
                    spriteNum = 2;
                } else if (spriteNum == 2) {
                    spriteNum = 1;
                }
                spriteCounter = 0;
            }
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;
        switch (direction) {
            case "up":
                if (spriteNum == 1) {
                    image = up1;
                }
                if (spriteNum == 2) {
                    image = up2;
                }
                break;
            case "down":
                if (spriteNum == 1) {
                    image = down1;
                }
                if (spriteNum == 2) {
                    image = down2;
                }
                break;
            case "left":
                if (spriteNum == 1) {
                    image = left1;
                }
                if (spriteNum == 2) {
                    image = left2;
                }
                break;
            case "right":
                if (spriteNum == 1) {
                    image = right1;
                }
                if (spriteNum == 2) {
                    image = right2;
                }
                break;
        }
        g2.drawImage(image, screenX, screenY, 33, 43, null);
    }
    
    public void pickUpObject(int index) {
        // IF index = 999 IT MEANS WE DIDN'T TOUCH ANY OBJECT
        if (index != 999) {
            String objectName = gp.obj[index].name;
            switch (objectName) {
                case "Key":
                    gp.playSE(1);
                    hasKey++;
                    gp.obj[index] = null;
                    System.out.println("Key: " + hasKey);
                    break;
                case "Door":
                    if (hasKey > 0) {
                        gp.playSE(3);
                        gp.obj[index] = null;
                        hasKey--;
                        System.out.println("Key: " + hasKey);
                    }
                    break;
                case "Boots":
                    gp.playSE(2);
                    speed += 1;
                    gp.obj[index] = null;
                    break;
            }
        }
    }
}