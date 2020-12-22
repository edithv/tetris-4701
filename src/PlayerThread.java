import java.util.concurrent.Callable;

public class PlayerThread extends Thread {

  private Thread thread;
  private double[] weights;
  private String threadName;
  private int totalRowsCleared = 0;
  private long randomSeed;
  private AtomicInteger valueToUpdate;

  public PlayerThread(String threadName, long randomSeed, double[] weights, AtomicInteger valueToUpdate) {
    this.threadName = threadName;
    this.weights = weights;
    this.randomSeed = randomSeed;
    this.valueToUpdate = valueToUpdate;
  }

  private double computeFitness(AdvancedState oldState, AdvancedState newState, int move, int rowsCleared) {
    int piece = oldState.getNextPiece();
    int orient = oldState.legalMoves()[move][AdvancedState.ORIENT];
    int slot = oldState.legalMoves()[move][AdvancedState.SLOT];
    int pieceWidth = oldState.pWidth[piece][orient];
    int pieceHeight = oldState.pHeight[piece][orient];
    int landingHeight = Fitness.arrayMax(oldState.top, slot, slot + pieceWidth) + pieceHeight / 2;

    int rowsEliminated = newState.getRowsCleared() - oldState.getRowsCleared() + rowsCleared;
    int bumpiness = newState.getBumpiness();
    int numHoles = newState.getNumHoles();
    int wellSum = newState.getWellSum();

    double fitness = weights[Constant.LANDING_HEIGHT] * landingHeight
        + weights[Constant.ROW_ELIMINATED] * rowsEliminated + weights[Constant.NUM_HOLES] * numHoles
        + weights[Constant.BUMPINESS] * bumpiness + weights[Constant.WELL_SUM] * wellSum;

    return fitness;
  }

  private double computeFitnessWithLookAhead(AdvancedState oldState, AdvancedState newState) {
    int rowsCleared = newState.getRowsCleared() - oldState.getRowsCleared();
    double totalFitness = 0;
    // Look-ahead try all possible move
    for (int i = 0; i < AdvancedState.N_PIECES; i++) {
      newState.setNextPiece(i);

      // Find best move if the next piece is i
      double tempBestFitness = -Double.MAX_VALUE;
      int tempBestMove = -1;
      for (int move = 0; move < newState.legalMoves().length; move++) {
        AdvancedState lookAheadState = newState.clone();
        lookAheadState.makeMove(move);
        if (lookAheadState.hasLost()) {
          continue;
        }

        double fitness = computeFitness(newState, lookAheadState, move, rowsCleared);
        if (fitness > tempBestFitness) {
          tempBestFitness = fitness;
          tempBestMove = move;
        }
      }

      if (tempBestMove != -1) {
        totalFitness += tempBestFitness;
      }

    }
    return totalFitness;
  }

  private double computeFitnessWithTwoLookAhead(AdvancedState oldState, AdvancedState newState,
      AdvancedState nextState) {
    int rowsCleared = nextState.getRowsCleared() - newState.getRowsCleared() - oldState.getRowsCleared();
    double totalFitness = 0;

    // look-two-ahead try all possible moves
    for (int i = 0; i < AdvancedState.N_PIECES; i++) {
      newState.setNextPiece(i);
      // Find best move if the next piece is i
      double tempBestFitness = -Double.MAX_VALUE;
      int tempBestMove = -1;
      for (int move = 0; move < newState.legalMoves().length; move++) {
        AdvancedState lookAheadState = newState.clone();
        lookAheadState.makeMove(move);
        if (lookAheadState.hasLost()) {
          continue;
        } else {
          for (int moves = 0; moves < nextState.legalMoves().length; moves++) {
            AdvancedState twoAheadState = lookAheadState.clone();
            twoAheadState.makeMove(moves);
            if (twoAheadState.hasLost()) {
              continue;
            }
            double fitness = computeFitness(lookAheadState, twoAheadState, moves, rowsCleared);
            if (fitness > tempBestFitness) {
              tempBestFitness = fitness;
              tempBestMove = moves;
            }
          }
        }

      }
      if (tempBestMove != -1) {
        totalFitness += tempBestFitness;
      }
    }
    return totalFitness;
  }

  // implement this function to have a working system
  private int pickMove(AdvancedState state, int[][] legalMoves) {
    double bestFitness = -Double.MAX_VALUE;
    int bestMove = 0;

    // System.out.println("rows: " + state.getRowsCleared());
    // System.out.println("Start picking moves");
    for (int move = 0; move < legalMoves.length; move++) {
      AdvancedState cs = state.clone();
      cs.makeMove(move);
      if (cs.hasLost()) {
        continue;
      }
      double fitness;
      if (state.getHighestColumn() > 10) {
        fitness = computeFitnessWithLookAhead(state, cs);
      } else {
        fitness = computeFitness(state, cs, move, 0);
      }
      if (fitness > bestFitness) {
        bestFitness = fitness;
        bestMove = move;
      }
    }

    return bestMove;
  }

  private int pickMoveForTwoAhead(AdvancedState state, int[][] legalMoves) {
    double bestFitness = -Double.MAX_VALUE;
    int bestMove = 0;

    for (int move = 0; move < legalMoves.length; move++) {
      AdvancedState cs = state.clone();
      cs.makeMove(move);
      if (cs.hasLost()) {
        continue;
      } else {
        int[][] legal = cs.legalMoves();
        for (int moves = 0; moves < legal.length; moves++) {
          AdvancedState css = cs.clone();
          css.makeMove(moves);
          if (css.hasLost()) {
            continue;
          } else {
            double fitness;
            if (cs.getHighestColumn() > 10) {
              fitness = computeFitnessWithTwoLookAhead(state, cs, css);
            } else {
              fitness = computeFitness(cs, css, moves, 0);
            }
            if (fitness > bestFitness) {
              bestFitness = fitness;
              bestMove = move;
            }
          }
        }
      }

    }

    return bestMove;
  }

  /*
   * public static void main(String[] args) { State s = new State(); new
   * TFrame(s); PlayerSkeleton p = new PlayerSkeleton(); while(!s.hasLost()) {
   * s.makeMove(p.pickMove(s,s.legalMoves())); s.draw(); s.drawNext(0,0); try {
   * Thread.sleep(300); } catch (InterruptedException e) { e.printStackTrace(); }
   * } System.out.println("You have completed "+s.getRowsCleared()+" rows."); }
   */

  public void run() {

    try {
      AdvancedState s = new AdvancedState(randomSeed);
      while (!s.hasLost()) {
        s.makeMove(pickMove(s, s.legalMoves()));
        totalRowsCleared = s.getRowsCleared();
      }
      valueToUpdate.updateValue(totalRowsCleared);
      System.out.println("Finished: " + totalRowsCleared);
    } catch (Exception e) {
      System.out.println("ERROR: Thread failed to update fitness value");
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void start() {
    if (thread == null) {
      thread = new Thread(this, threadName);
      thread.start();
    }
  }

  // Setters + getters
  public int getTotalRowsCleared() {
    return totalRowsCleared;
  }

}
