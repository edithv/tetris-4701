import java.util.Random;

public class Constant {
  public static final int POPULATION_SIZE = 50;
  public static final int PERCENTAGE_CROSS_OVER = 10;
  public static final int PERCENTAGE_MUTATION = 10;
  public static final int NUMB_FEATURES = 5;
  public static final int NUMB_ITERATIONS = 20000;
  public static final int NUMB_GAMES_PER_UPDATE = 5;

  // Indices of features
  public static final int LANDING_HEIGHT = 0;
  public static final int ROW_ELIMINATED = 1;
  public static final int NUM_HOLES = 2;
  public static final int BUMPINESS = 3;
  public static final int WELL_SUM = 4;

  public static final int[] FEATURE_TYPE = { -1, 1, -1, -1, -1 };

  public static final long[] SEEDS = { (new Random()).nextLong(), (new Random()).nextLong(), (new Random()).nextLong(),
      (new Random()).nextLong(), (new Random()).nextLong() };
}
