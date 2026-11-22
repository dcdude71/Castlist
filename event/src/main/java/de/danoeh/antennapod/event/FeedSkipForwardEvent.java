package de.danoeh.antennapod.event;

public class FeedSkipForwardEvent {
    public final long feedId;

    public FeedSkipForwardEvent(long feedId) {
        this.feedId = feedId;
    }
}
