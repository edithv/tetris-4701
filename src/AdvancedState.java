import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class AdvancedState extends State {

  private int randomSeed;
  private Random rand;

  public AdvancedState(long randomSeed) {
    this.rand = new Random(randomSeed);
    this.nextPiece = randomPiece();
  }

  public int getAggregateHeight() {
    int aggregateHeight = IntStream.of(top).sum();
    return aggregateHeight;
  }

  public int getNumHoles() {
    int numHoles = 0;
    for (int j = 0; j < COLS; j++) {
      if (top[j] != 0) {
        for (int i = top[j] - 1; i >= 0; i--) {
          if (field[i][j] == 0) {
            numHoles++;
          }
        }
      }
    }
    return numHoles * 10;
  }

  public int getBumpiness() {
    int bumpiness = 0;
    for (int i = 0; i < COLS - 1; i++) {
      bumpiness += Math.abs(top[i] - top[i + 1]);
    }
    return bumpiness;
  }

  public int getHighestColumn() {
    int highestCol = 0;
    for (int i = 0; i < COLS; i++) {
      highestCol = Math.max(highestCol, top[i]);
    }
    return highestCol;
  }

  public int getWellSum() {
    int next, prev, wellSum = 0;
    cleanField();
    for (int j = 0; j < COLS; j++) {
      for (int i = ROWS - 1; i >= 0; i--) {
        if (field[i][j] == 0) {
          if (j == 0 || field[i][j - 1] != 0) {
            if (j == COLS - 1 || field[i][j + 1] != 0) {
              int wellHeight = i - top[j] + 1;
              wellSum += wellHeight * (wellHeight + 1) / 2;
            }
          }
        } else {
          break;
        }
      }
    }
    return wellSum;
  }

  public void cleanField() {
    for (int j = 0; j < COLS; j++) {
      for (int i = top[j]; i < ROWS; i++) {
        field[i][j] = 0;
      }
    }
  }

  @Override
  protected int randomPiece() {
    if (this.rand == null) {
      return (int) (Math.random() * N_PIECES);
    }
    return (int) (this.rand.nextDouble() * N_PIECES);
  }

  public int[][] copy2DArray(int[][] arr) {
    int[][] copy = new int[arr.length][arr[0].length];
    for (int i = 0; i < arr.length; i++) {
      for (int j = 0; j < arr[i].length; j++) {
        copy[i][j] = arr[i][j];
      }
    }
    return copy;
  }

  public void setNextPiece(int nextPiece) {
    this.nextPiece = nextPiece;
  }

  public AdvancedState clone() {
    AdvancedState clonedState = new AdvancedState(randomSeed);
    clonedState.field = copy2DArray(getField());
    clonedState.top = Arrays.copyOf(getTop(), getTop().length);
    clonedState.nextPiece = getNextPiece();
    clonedState.setRowsCleared(getRowsCleared());
    return clonedState;
  }

}
