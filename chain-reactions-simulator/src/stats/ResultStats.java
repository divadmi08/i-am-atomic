package stats;

import model.Outcome;

public class ResultStats {

    public long stable;
    public long failed;
    public long critical;
    public long explosion;

    public void add(Outcome o) {

        switch (o) {
            case STABLE -> stable++;
            case FAILED -> failed++;
            case CRITICAL -> critical++;
            case EXPLOSION -> explosion++;
        }
    }

    public void merge(ResultStats other) {
        stable += other.stable;
        failed += other.failed;
        critical += other.critical;
        explosion += other.explosion;
    }

    public long total() {
        return stable + failed + critical + explosion;
    }
}
