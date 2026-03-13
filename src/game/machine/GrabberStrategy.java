package game.machine;

/**
 * Dummy-Strategy für den Grabber.
 * Die eigentliche Logik ist in Grabber.tick() implementiert,
 * da der Grabber auf mehrere Tiles zugreifen muss (Multi-Lock).
 */
public class GrabberStrategy implements ProductionStrategy {
   @Override
   public void produce(BaseMachine machine) {
      // Wird nicht verwendet — Grabber überschreibt tick() direkt
   }
}
