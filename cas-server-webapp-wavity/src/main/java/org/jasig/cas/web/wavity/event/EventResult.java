package org.jasig.cas.web.wavity.event;

/**
 * This enumeration defines the Event results.
 */
public enum EventResult
{
  /**
   * A success event.
   */
  SUCCESS("success"),
  /**
   * A failure event.
   */
  FAILURE("failure");
  /**
   * Constructs an instance of this class.
   *
   * @param name The name of the result.
   */
  EventResult(String name)
  {
    this.name = name;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return name;
  }

  // The name of the event.
  private String name;
}
