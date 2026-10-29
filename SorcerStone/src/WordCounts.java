import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Hash table from lowercase ASCII words to counts. Keys are stored back to back
 * in one byte array, so counting allocates nothing per word.
 */
public final class WordCounts {
    // Enough for one book (Harry Potter 1 has 5 764 distinct words) without growing.
    private static final int INITIAL_WORDS = 8192;

    private byte[] letters = new byte[INITIAL_WORDS * 8];
    // Word i occupies letters[offsets[i]] .. letters[offsets[i + 1] - 1].
    private int[] offsets = new int[INITIAL_WORDS + 1];
    private int[] counts = new int[INITIAL_WORDS];
    // Open addressing with linear probing: word index + 1, or 0 for a free slot.
    private int[] slots = new int[INITIAL_WORDS * 2];
    private int size;

    WordCounts() {
    }

    public int size() {
        return size;
    }

    public String word(int index) {
        checkIndex(index);
        return new String(letters, offsets[index], offsets[index + 1] - offsets[index],
                StandardCharsets.US_ASCII);
    }

    public int count(int index) {
        checkIndex(index);
        return counts[index];
    }

    /** Adds one occurrence of word[0..length); hash must be the String.hashCode of it. */
    void increment(byte[] word, int length, int hash) {
        int mask = slots.length - 1;
        for (int slot = spread(hash) & mask; ; slot = (slot + 1) & mask) {
            int index = slots[slot] - 1;
            if (index < 0) {
                slots[slot] = add(word, length) + 1;
                if (size == counts.length) {
                    grow();
                }
                return;
            }
            if (sameWord(index, word, length)) {
                counts[index] = Math.incrementExact(counts[index]);
                return;
            }
        }
    }

    /** Report order: higher count first, then alphabetical. */
    boolean ranksBefore(int left, int right) {
        if (counts[left] != counts[right]) {
            return counts[left] > counts[right];
        }
        return Arrays.compare(letters, offsets[left], offsets[left + 1],
                letters, offsets[right], offsets[right + 1]) < 0;
    }

    private int add(byte[] word, int length) {
        int start = offsets[size];
        if (start + length > letters.length) {
            letters = Arrays.copyOf(letters, Math.max(letters.length * 2, start + length));
        }
        System.arraycopy(word, 0, letters, start, length);
        offsets[size + 1] = start + length;
        counts[size] = 1;
        return size++;
    }

    private boolean sameWord(int index, byte[] word, int length) {
        int start = offsets[index];
        return offsets[index + 1] - start == length
                && Arrays.equals(letters, start, start + length, word, 0, length);
    }

    private void grow() {
        counts = Arrays.copyOf(counts, counts.length * 2);
        offsets = Arrays.copyOf(offsets, counts.length + 1);
        slots = new int[counts.length * 2];
        int mask = slots.length - 1;
        for (int index = 0; index < size; index++) {
            int slot = spread(hashOf(index)) & mask;
            while (slots[slot] != 0) {
                slot = (slot + 1) & mask;
            }
            slots[slot] = index + 1;
        }
    }

    private int hashOf(int index) {
        int hash = 0;
        for (int i = offsets[index]; i < offsets[index + 1]; i++) {
            hash = 31 * hash + letters[i];
        }
        return hash;
    }

    private static int spread(int hash) {
        return hash ^ (hash >>> 16);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
    }
}
