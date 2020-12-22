//This program is forked from the Github repository:
//    Wcabynessa/Tetris-AI
// This original program was heavily adjusted to meet our needs.

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class GeneticAlgorithm {

  public static ArrayList<Person> population;

  public static void main(String[] args) {
    System.out.println("Beginning GA");
    State.initializeLegalMoves();
    for (double i : GeneticSearch()) {
      System.out.println(i);
    }
  }

  private static void saveToFile(int iteration) {
    try {
      PrintWriter out = new PrintWriter(new FileOutputStream("output.txt"));
      out.println(iteration);
      out.println("Best value: " + population.get(0).getFitness());
      out.println("Worst value: " + population.get(population.size() - 1).getFitness());
      int index = 0;
      for (Person p : population) {
        double[] weights = population.get(index).weights;
        for (double w : weights) {
          out.print(w + " ");
        }
        out.println();
        index++;
      }
      out.close();
    } catch (Exception e) {
      System.out.println("Couldn't save file");
    }
  }

  private static double[] GeneticSearch() {
    InitializePopulation();
    refinePopulation();
    saveToFile(-1);

    for (int iteration = 0; iteration < Constant.NUMB_ITERATIONS; iteration++) {
      System.out.println("#------------------- Starting Iteration # + " + iteration + "-----------------------#");
      expandPopulationByCrossOver();
      expandPopulationByMutation();
      refinePopulation();

      // Logging
      System.out.println("# Current best value: " + population.get(0).getFitness());
      System.out.println("# Current min value: " + population.get(population.size() - 1).getFitness());
      double[] weights = population.get(0).weights;
      for (double i : weights) {
        System.out.print(i + " ");
      }
      System.out.println();

      saveToFile(iteration);
    }

    return population.get(0).weights;

  }

  private static void InitializePopulation() {
    population = new ArrayList<Person>();

    double[][] weightsSet = {
        { -4.856393412802155, 3.5035969996231951, -5.696079737154641, -2.880295692564255, -4.856393412802155 },
        { -6.353544509908005, 2.8153473729915044, -8.628591938844059, -7.6261583212092905, -6.353544509908005 },
        { -5.447335891581961, 8.875201136503023, -4.209984658848587, -3.4249029633214434, -5.447335891581961 },
        { -1.6079163204088776, 0.07830273595682002, -3.9763720595731487, -3.392489979806549, -1.6079163204088776 },
        { -9.100653144629398, 4.402909393226468, -5.708796370057478, -9.34658732150928, -9.100653144629398 },
        { -5.576697856421292, 1.977271233134471, -9.72612929387645, -6.471947186286485, -5.576697856421292 },
        { -2.839548921847872, 9.175617948954585, -3.6898333138035087, -4.1842417277399635, -2.839548921847872 },
        { -6.212299436204987, 4.694847016310222, -3.711280319354274, -3.082390144105074, -6.212299436204987 },
        { -2.29228711620576, 9.79450773957667, -7.417125307425103, -8.144555230558607, -2.29228711620576 }

    };
    for (double[] weights : weightsSet) {
      Person elTetris = new Person(weights);
      elTetris.updateFitness();
      population.add(elTetris);
    }

    for (int i = 0; i < Constant.POPULATION_SIZE; i++) {
      population.add(new Person());
    }

    ThreadController threadMaster = ThreadController.getInstance();
    threadMaster.waitFinishUpdate();
  }

  private static void refinePopulation() {
    Collections.sort(population);
    while (population.size() > Constant.POPULATION_SIZE) {
      population.remove(population.size() - 1);
    }
  }

  private static void expandPopulationByCrossOver() {
    Vector subjects = new Vector<Person>();
    for (int i = 0; i < Constant.PERCENTAGE_CROSS_OVER * Constant.POPULATION_SIZE / 100; i++) {
      int subject1 = Fitness.randomInt(population.size());
      int subject2 = Fitness.randomInt(population.size());
      if (subject1 != subject2) {
        subjects.add(Person.crossOver(population.get(subject1), population.get(subject2)));
      }
    }
    subjects.forEach(subject -> population.add((Person) subject));

    ThreadController threadMaster = ThreadController.getInstance();
    threadMaster.waitFinishUpdate();
  }

  private static void expandPopulationByMutation() {
    Vector subjects = new Vector<Integer>();
    while (subjects.size() < Constant.PERCENTAGE_MUTATION * Constant.POPULATION_SIZE / 100) {
      int subject = Fitness.randomInt(Constant.POPULATION_SIZE);
      if (!subjects.contains(subject)) {
        subjects.add(subject);
      }
    }

    for (int i = 0; i < subjects.size(); i++) {
      int subject = Fitness.randomInt(subjects.size());
      int featureIndex = Fitness.randomInt(Constant.NUMB_FEATURES);

      population.add(Person.mutate(population.get(subject), featureIndex));
    }

    ThreadController threadMaster = ThreadController.getInstance();
    threadMaster.waitFinishUpdate();
  }
}

class Person implements Comparable<Person> {
  public double[] weights;
  private AtomicInteger fitness = new AtomicInteger(0);

  public Person() {
    this.randomWeightVector();
    this.updateFitness();
  }

  public Person(double[] weights) {
    this.weights = weights;
  }

  private void randomWeightVector() {
    weights = new double[Constant.NUMB_FEATURES];
    for (int i = 0; i < Constant.NUMB_FEATURES; i++) {
      weights[i] = Math.abs(Fitness.randomReal() * 10) * Constant.FEATURE_TYPE[i];
    }
  }

  public void updateFitness() {
    ThreadController threadMaster = ThreadController.getInstance();

    for (int i = 0; i < Constant.NUMB_GAMES_PER_UPDATE; i++) {
      long randomSeed = Constant.SEEDS[i];
      String threadName = this.toString() + " #" + i;

      PlayerThread game = new PlayerThread(threadName, randomSeed, weights, fitness);
      threadMaster.submitTask(game);
    }
  }

  public static Person crossOver(Person self, Person other) {
    double[] weights = Arrays.copyOf(self.weights, self.weights.length);
    for (int i = 0; i < weights.length; i++) {
      if (Fitness.flipCoin()) {
        weights[i] = other.weights[i];
      }
    }
    Person child = new Person(weights);
    child.updateFitness();
    return child;
  }

  public static Person mutate(Person self, int mutateLocation) {
    double[] weights = Arrays.copyOf(self.weights, self.weights.length);
    weights[mutateLocation] += Fitness.randomReal() * 2;
    Person child = new Person(weights);
    child.updateFitness();
    return child;
  }

  public int compareTo(Person other) {
    return other.fitness.getValue() - this.fitness.getValue();
  }

  public void setWeights(double[] weights) {
    this.weights = weights;
  }

  public double[] getWeights() {
    return weights;
  }

  public AtomicInteger getFitness() {
    return this.fitness;
  }

  public Person clone() {
    return new Person(Arrays.copyOf(weights, weights.length));
  }

  public String toString() {
    String text = "";
    for (double weight : weights) {
      text += "|" + weight;
    }
    return text;
  }

}
