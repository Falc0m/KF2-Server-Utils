package pl.fanta.utils;

import java.util.function.BooleanSupplier;


// my implementation of conditional sleep with functional interface
public final class ConditionalSleep {

    public static void sleepUntil(final BooleanSupplier condition, final int timeout, final int interval) {
        final long timeoutSystem = System.currentTimeMillis() + timeout;

        try {
            while (!condition.getAsBoolean() && System.currentTimeMillis() <= timeoutSystem) {
                Thread.sleep(interval);
            }
            System.out.println("Condition true! " + condition.getAsBoolean());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
