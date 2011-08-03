package sample;

import org.junit.runners.model.InitializationError;

public class Example {

	public void shouldTriggerRedundantThrows() throws InitializationError, Exception {}

	public void shouldNotTriggerRedundantThrows() throws InitializationError {}

	public void workingRedundantThrowsExample() throws IllegalArgumentException, Exception {}

	public void workingDoesNotTriggerRedundantThrowsExample() throws IllegalArgumentException {}
}