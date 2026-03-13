package game.machine;

/**
 * Strategy-Pattern-Interface für verschiedene Produktionstypen.
 */
public interface ProductionStrategy {
    void produce(BaseMachine machine);
}
