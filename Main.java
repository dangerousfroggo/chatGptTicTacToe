import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.json.simple.JSONArray; 
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

class Game {

/**
void method that prints out the grid given a 2d array with containing current grid
@param takes a 2d string array, corresponding to the current positions of X and O
@return void

*/

  public static void displayGrid(String[][] grid) {
      String spacing = "   "; /* spacing for grid */
      System.out.println("\n");

      int slot = 1;
      for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
              System.out.print(spacing + grid[i][j] + spacing);
              if (j < 2) {
                  System.out.print("|");
              }
            
              slot++;
          }
          System.out.println("\n");
          if (i < 2) {
              System.out.println(" --------------------- ");
          }
      }
  }


/**
method that calculates the winner of the game
scans through the grid, rows first, then columns, then diagonals
@param takes a 2d string array, corresponding to the current positions of X and O
@return a string tie, x, or o, corresponding to the winner of the game.
*/
    public static String winCalculator(String[][] grid) {
    String win = "noWinner";
    int rowScanner;
    for (rowScanner = 0; rowScanner < 3; rowScanner += 1) {
        if (grid[rowScanner][0].equals(grid[rowScanner][1]) && grid[rowScanner][1].equals(grid[rowScanner][2])) {
            if (grid[rowScanner][0].equals("X")) {
                win = "X";
            } else if (grid[rowScanner][0].equals("O")) {
                win = "O";
            }
        }
    }

    int columnScanner;
    for (columnScanner = 0; columnScanner < 3; columnScanner += 1) {
        if (grid[0][columnScanner].equals(grid[1][columnScanner]) && grid[1][columnScanner].equals(grid[2][columnScanner])) {
            if (grid[0][columnScanner].equals("X")) {
                win = "X";
            } else if (grid[0][columnScanner].equals("O")) {
                win = "O";
            }
        }
    }
/** 
scans diagonals and determines winner based on center of the grid's value
*/
  if(grid[0][0].equals(grid[1][1]) && grid[1][1].equals(grid[2][2])){
    win = grid[0][0];
  }
  else if(grid[0][2].equals(grid[1][1]) && grid[1][1].equals(grid[2][0])){
    win = grid[1][1];
  }

    
  return win;
    
    
  }

/**
  inputs X or O into specified position on the current grid
  @param two coordinates as intergers, 2d string array, string "X" or "Y" corresponding to the 
  desired input
  @return 2d string array, the updated grid
*/
  public static String[][] gridInput(int slot, String[][] grid, String character) {
      if (slot >= 1 && slot <= 9) {
          int row = (slot - 1) / 3;
          int col = (slot - 1) % 3;

          if (grid[row][col].equals("X") || grid[row][col].equals("O")) {
              System.out.println("Slot already taken. Please try again");
          } else {
              grid[row][col] = character;
          }
      } else {
          System.out.println("Invalid slot. Please enter a number between 1 and 9.");
      }

      return grid;
  }
  
  
 
}
class ChatGptInterface {

  /** 
  creates a string prompt that is sent to the chat gpt api
  @param takes a string, the training text followed by the current grid as a coordinate system
  @returns a string, the prompt for the chat gpt api
  */
  public static String createPromptForGpt(String[][] grid, String playerType) {
    String prompt = "you are a chat bot, and your job is to play a tic tac toe game against me. do not return any text, only the grid. you are " + playerType +  " and therefore can only make moves as " + playerType + ". you can only make one move per turn. if you make a move that puts you in a spot that is already taken, you will lose. the current grid contains numbers 1-9 as fillers, so the user youre playing against can enter which slot they would like to place their character in. make sure the grid you return still contains these fillers in their original location, unless replaced by an X or an O. the grid is as follows: ";
    prompt += "here is the current grid: ";
    prompt += Arrays.deepToString(grid);
    return prompt;
  }
  
  /**
  after receiving a response from the api, parses it back into a 2d array to be used in the gridInput method
  @param takes a string, the response from the api
  @return 2d array to be used in gridInput method
  */
  
  public static String[][] parseApiResponse(String[][] grid, String response) {
      try {
          JSONParser parser = new JSONParser();
          JSONObject jsonResponse = (JSONObject) parser.parse(response);

          JSONArray choices = (JSONArray) jsonResponse.get("choices");
          if (choices.size() > 0) {
              JSONObject firstChoice = (JSONObject) choices.get(0);
              String text = (String) firstChoice.get("text");

              // Extract the 2D array from the text
              String[] rows = text.split("\\], \\[");
              for (int i = 0; i < rows.length; i++) {
                  rows[i] = rows[i].replaceAll("[\\[\\]]", "").trim();
                  String[] cells = rows[i].split(",\\s*");

                  for (int j = 0; j < cells.length && j < grid[i].length; j++) {
                      grid[i][j] = cells[j].trim();
                  }
              }
          }
      } catch (ParseException e) {
          e.printStackTrace();
      }

      return grid;
  }
  /**
  using the generated prompt and the current grid, sends a prompt to the chatgptapi.
  if api call succeeds, parses the respose through the parseApiResponse method and returns the updated grid
  if api call fails, returns the unchanged grid
  @param takes the current grid
  @return grid after the ai makes a move

  */

  
  public static String[][] generateAiMove(String[][] grid, String aiCharType) { 
        String prompt = createPromptForGpt(grid, aiCharType);
        String requestBody = "{\"prompt\": \"" + prompt + "\", \"max_tokens\": 1000}";
        String openAiApiUrl = "https://api.openai.com/v1/engines/text-davinci-003/completions";
        String apiKey = System.getenv("apiKey");
      /* modified chatgpt boilerplate starts here (from openai documentation)*/
        try {
            URL url = new URL(openAiApiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey); ///environment variable, must remember to add to git
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            System.out.println("~api call successful~");

            try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
                StringBuilder response = new StringBuilder();
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }

                System.out.println(response);
                String[][] generatedMove = parseApiResponse(grid,response.toString());
                //System.out.println(Arrays.deepToString(generatedMove));
                return generatedMove;
            }
      /*and ends here*/
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("~failed api call"); //test 
        }

        return grid; // if api call fails, returns current grid with no changes//
        
    }



}
class Main {
  /**
  main method that runs the game
  */
 

  /**
    void method with no return type, clears the screen when called upon
  */
  public static void clearScreen() {  
    System.out.print("\033[H\033[2J");  
    System.out.flush();  
   }
  public static String randomPlayerTypePicker(){
    double randomNum = Math.random();
    if (randomNum <= 0.5){
      return "X";
    }
    else{
      return "O";
    }
    
  }

  public static void delay(int ms) {
      try {
          Thread.sleep(ms);
      } catch (InterruptedException e) {
          
          e.printStackTrace();
      }
  }
  
  /*
  main method for the game 
  */
  public static void main(String[] args) {
    String humanPlayerType = randomPlayerTypePicker();
    String aiPlayerType = "";
    if (humanPlayerType.equals("X")){
      aiPlayerType = "O";
    } else {
      aiPlayerType = "X";
    }
  
    
      
    boolean playingGame = true; ///boolean to keep the game running
    while(playingGame){
    Scanner sc = new Scanner(System.in);
    System.out.println("hello human. welcome to tic tac toe. would you like to play against chadgpt or another human player? type either 'chadgpt' or 'human'.");
    String opponent = sc.nextLine();
    boolean isGptFirst;
      
    if (opponent.equals("chadgpt")){
   
      String[][] currentGrid = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}}; 
      System.out.println("you are playing against chadgpt.");
      System.out.println("you are " + humanPlayerType + ". ChadGPT is " + aiPlayerType);
      delay(2000);
      System.out.println("here is your grid");
    
    
      if(aiPlayerType.equals("X")){
        isGptFirst = true;
      } else {
        isGptFirst = false;
      }
    
      while(true){
        clearScreen();
        Game.displayGrid(currentGrid);
        String winner;
        if (isGptFirst){
          System.out.println("chadgpt is making a move...");
          currentGrid = ChatGptInterface.generateAiMove(currentGrid, aiPlayerType);
        } else {
            System.out.println("it is your turn. enter the slot you would like to place your character in.");
            int slotx = sc.nextInt();
            currentGrid = Game.gridInput(slotx, currentGrid, humanPlayerType);
        }

        winner = Game.winCalculator(currentGrid);
        if(!winner.equals("noWinner")){
          clearScreen();
          Game.displayGrid(currentGrid);
          System.out.println(winner + " has won the game!");
          break;
        } else if (winner.equals("Tie")){
          clearScreen();
          Game.displayGrid(currentGrid);
          System.out.println("the game is a tie!");
          break;
  
        
        
      }
      
      isGptFirst = !isGptFirst;
    }   
  }
    ///if opponent is human, prompts user for input. ///
    
    else if(opponent.equals("human")){
      System.out.println("hello human you are playing chadgpt against another human");
      String[][] grid = {{" ", " ", " "}, {" ", " ", " "},{" ", " ", ""}};
      Game.displayGrid(grid);
      while (true){
        System.out.println("Player X, enter the slot where you would like to place your X on a new line");
        grid = Game.gridInput(sc.nextInt(),grid,"X");
        Game.displayGrid(grid);

        //checks for winner after X turn
        if(Game.winCalculator(grid) == "X"){
        System.out.println("X won");
        break;

        } else if (Game.winCalculator(grid) == "O"){
        System.out.println("O won");
        break;
        }

        System.out.println("Player O, enter the slot where you would like to place your O on a new line");
        grid = Game.gridInput(sc.nextInt(),grid,"O");
        Game.displayGrid(grid);
        ///checks for winner after O turn
        if(Game.winCalculator(grid) == "X"){
        System.out.println("X won");
        break;
        } else if (Game.winCalculator(grid) == "O"){
        System.out.println("O won");
        break;
        }
        System.out.println("tie");
        
      }
      System.out.println("~game completed"); 
      
      
      
    }

  
  }
    
  }

  }