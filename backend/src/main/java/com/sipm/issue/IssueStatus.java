package com.sipm.issue;

public enum IssueStatus {
    OPEN(1), IN_PROGRESS(2), RESOLVED(3), CLOSED(4);

    private final short rank;

    IssueStatus(int rank) {
        this.rank = (short) rank;
    }

    public short rank() { return rank; }
}
