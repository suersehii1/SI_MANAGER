package com.sipm.issue;

public enum IssuePriority {
    LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);

    private final short rank;

    IssuePriority(int rank) {
        this.rank = (short) rank;
    }

    public short rank() { return rank; }
}
