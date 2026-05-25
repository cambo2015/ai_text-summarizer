#!/usr/bin/env python3
import sys
import re
import argparse
from collections import Counter

def main():
    parser = argparse.ArgumentParser(description="Simple cross-platform word frequency CLI")
    parser.add_argument("--top", type=int, default=50, help="Number of top words to show")
    parser.add_argument("--min-len", type=int, default=2, help="Minimum word length")
    args = parser.parse_args()

    text = sys.stdin.read().lower()
    words = re.findall(r"[a-z']+", text)

    counts = Counter(w for w in words if len(w) >= args.min_len)

    STOPWORDS = {
        "a","an","the","and","or","but",
        "to","of","in","on","for","with","at","from","by",
        "is","are","was","were","be","been","being",
        "i","you","we","they","it",
        "that","this","these","those",
        "have","has","had",
        "do","does","did",
        "so","just","like","yeah","ok","okay","um","uh",
        "all","will",
        "let's","can't","won't","would","could","should","couldn't","ain't","shouldn't","wouldn't","shall"
    }

    for word, count in counts.most_common(args.top):
        if(word not in STOPWORDS):
            print(f"{word} {count}")

if __name__ == "__main__":
    main()
