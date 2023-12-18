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
  
    String spacing = "       "; /* spacing for grid */
    System.out.println("\n");
    System.out.println(spacing + " 1" + "      " + " 2" + "      " + " 3");
    System.out.println("\n");
    
    System.out.println("1" + spacing + grid[0][0] + spacing + grid[0][1] + spacing + grid[0][2]);
    System.out.println("\n");
    
    System.out.println("2" + spacing + grid[1][0] + spacing + grid[1][1] + spacing + grid[1][2]);
    System.out.println("\n");
  
    System.out.println("3" + spacing + grid[2][0] + spacing + grid[2][1] + spacing + grid[2][2]);
    
  }

/**
method that calculates the winner of the game
scans through the grid, rows first, then columns, then diagonals
@param takes a 2d string array, corresponding to the current positions of X and O
@return a string tie, x, or o, corresponding to the winner of the game.
*/
  public static String winCalculator(String[][] grid) {

/**
 scans rows 1-3 and checks for equivalency
*/
    String win = "no winner determined yet";
    int rowScanner;
    for(rowScanner = 0; rowScanner < 3; rowScanner+=1){
      if(grid[rowScanner][0].equals(grid[rowScanner][1]) && grid[rowScanner]  [1].equals(grid[rowScanner][2])){
        if(grid[rowScanner][0].equals("X")){
          win = "X";
        }
        else if(grid[rowScanner][0].equals("O")){
          win = "O";
        }
      }
    }
/**
  scans colums 1-3 and checks for equivalency
*/
    int columnScanner;
    for(columnScanner = 0; columnScanner <3; columnScanner +=1){
      if(grid[0][columnScanner].equals(grid[1][columnScanner]) && grid[1][columnScanner].equals(grid[2][columnScanner])){
        if(grid[0][columnScanner].equals("X")){
          win = "X";
        }
        else if(grid[columnScanner][0].equals("O")){
          win = "O";
        } else {
          win = "Tie";
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
  public static String[][] gridInput(int xPos, int yPos, String[][] grid, String character){
    grid[yPos][xPos] = character;

    return grid;
  }
  
  
 
}
class ChatGptInterface {

  /** 
  creates a string prompt that is sent to the chat gpt api
  @param takes a string, the training text followed by the current grid as a coordinate system
  @returns a string, the prompt for the chat gpt api
  */
  public static String createPromptForGpt(String[][] grid) {
    String prompt = "you are a chat bot, and your job is to play a tic tac toe game against me. do not return any text, only the grid. you are O, and therefore can only make moves as O. you can only make one move per turn. if you make a move that puts you in a spot that is already taken, you will lose";
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
                  rows[i] = rows[i].replaceAll("[\\[\\]]", "").replaceAll(",", "").trim();
                  String[] cells = rows[i].split(" ");
                  for (int j = 0; j < cells.length; j++) {
                      grid[i][j] = cells[j];
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

  
  public static String[][] generateAiMove(String[][] grid) { /*make private? */
        String prompt = createPromptForGpt(grid);
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
  public static void main(String[] args) {
    boolean playingGame = true;
    while(playingGame){
    Scanner sc = new Scanner(System.in);
    System.out.println("hello human do you wanna play against anothe human or be slayed by chadgpt?");
    String opponent = sc.nextLine();
    if (opponent.equals("chadgpt")){
    String[][] grid = {{" ", " ", " "}, {" ", " ", " "}, {" ", " ", " "}}; 
    System.out.println("hello chat u are playing tictactoe against chad gpt");
    
    Game.displayGrid(grid);
    System.out.println("Enter your next move (y,x)");
    
      while (true) {
      grid = Game.gridInput(sc.nextInt()-1,sc.nextInt()-1,grid,"X"); ///takes user input and inputs into grid as x
      if (Game.winCalculator(grid) == "X") {
        Game.displayGrid(grid);
        System.out.println("You won!");
        break;
      }
      grid = ChatGptInterface.generateAiMove(grid);
        
      if (Game.winCalculator(grid) == "O") {
        Game.displayGrid(grid);
        System.out.println("You lost!");
        break;
      }
      Game.displayGrid(grid);
      System.out.println("Enter your next move (y,x)");
    }
    } 


    
    ///if opponent is human, prompts user for input. ///
    
    else if(opponent.equals("human")){
      System.out.println("hello human you are playing chadgpt against another human");
      String[][] grid = {{" ", " ", " "}, {" ", " ", " "},{" ", " ", ""}};
      Game.displayGrid(grid);
      while (true){
        System.out.println("player X, enter input as (x,y)");
        grid = Game.gridInput(sc.nextInt()-1,sc.nextInt()-1,grid,"X");
        Game.displayGrid(grid);

        //checks for winner after X turn
        if(Game.winCalculator(grid) == "X"){
        System.out.println("x won");
        break;
        } else if (Game.winCalculator(grid) == "O"){
        System.out.println("o won");
        break;
        }

        System.out.println("player O, enter input as (x,y)");
        grid = Game.gridInput(sc.nextInt()-1,sc.nextInt()-1,grid,"O");
        Game.displayGrid(grid);
        ///checks for winner after O turn
        if(Game.winCalculator(grid) == "X"){
        System.out.println("x won");
        break;
        } else if (Game.winCalculator(grid) == "O"){
        System.out.println("o won");
        break;
        }
        System.out.println("tie");
        
      }
      System.out.println("~game completed"); //tests for game end
      
      
      
    }

  
  }
    
  }
}
