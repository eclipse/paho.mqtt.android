package org.eclipse.paho.android;

class TestCaseNotifier {

    private Throwable exception;

    public void storeException(Throwable exception) {
        this.exception = exception;
    }

    public synchronized void waitForCompletion(long timeout) throws Throwable {

        try {
            wait(timeout);
        }
        catch (InterruptedException ignored) {}

        if (exception != null) {
            throw exception;
        }

    }

}
