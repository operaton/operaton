#!/usr/bin/env python3
# Copyright 2025 the Operaton contributors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import subprocess
import sys
import argparse
import csv
from datetime import datetime
from collections import defaultdict

# Blacklist für Bot-Accounts oder unerwünschte Autoren
author_blacklist = {
    "github-actions[bot]",
    "dependabot[bot]",
    "Renovate Bot",
    "renovate[bot]",
    "GitHub",
    "GitHub Actions",
    "Copilot",
    "copilot-swe-agent[bot]"
}

# Mapping für Namens-/E-Mail-Disambiguierung
author_map = {
    "kthoms": "Karsten Thoms",
    "cmoellerherm": "Christopher Möllerherm",
    "Jaye": "Javad Malaquti",
    "cachescrubber": "Lars Uffmann",
    "CBorowski-dev": "Christian Borowski",
    "Datafluisteraar": "Steven Gort",
    "ahmedfarhat": "Ahmed Farhat",
    "juliateles99": "Julia Teles",
    "Brijeshthummar02": "Brijesh Thummar",
    "geduldia": "Alena Geduldig",
    "HamzaIddaoui": "Hamza Iddaoui",
    "Andreas \"Kungi\" Klein": "Andreas Klein",
    "zeguilherme99": "José Guilherme",
    "Noah": "Noah Guerin",
}

def run_git_log(since="2024-10-01"):
    """
    Runs git log to collect per-commit stats since the given date.
    Returns a list of tuples: (commit_date, files_changed, insertions, deletions)
    """
    git_log_cmd = [
        "git", "log", f"--since={since}", "--numstat", "--date=short", "--pretty=format:--%n%cd"
    ]
    result = subprocess.run(git_log_cmd, capture_output=True, text=True, check=True)
    lines = result.stdout.splitlines()

    stats = []
    current_date = None
    files_changed = 0
    insertions = 0
    deletions = 0
    commit_files = 0

    for line in lines:
        if line.startswith("--"):
            if current_date is not None:
                stats.append((current_date, commit_files, insertions, deletions))
            # Reset for new commit
            current_date = None
            commit_files = 0
            insertions = 0
            deletions = 0
        elif line and line[0].isdigit() and "\t" in line:
            parts = line.split("\t")
            if len(parts) >= 3:
                try:
                    ins = int(parts[0]) if parts[0] != '-' else 0
                    dels = int(parts[1]) if parts[1] != '-' else 0
                except ValueError:
                    ins = 0
                    dels = 0
                insertions += ins
                deletions += dels
                commit_files += 1
        elif line and line[:4].isdigit():
            # Date line
            current_date = line.strip()
    # Last commit
    if current_date is not None:
        stats.append((current_date, commit_files, insertions, deletions))
    return stats

def aggregate_monthly(stats):
    """
    Aggregates stats per month.
    Returns a dict: {month: {'files_changed': int, 'insertions': int, 'deletions': int, 'commits': int}}
    """
    monthly = defaultdict(lambda: {'files_changed': 0, 'insertions': 0, 'deletions': 0, 'commits': 0, 'files_per_commit': []})
    for date, files, ins, dels in stats:
        month = date[:7]  # YYYY-MM
        monthly[month]['files_changed'] += files
        monthly[month]['insertions'] += ins
        monthly[month]['deletions'] += dels
        monthly[month]['commits'] += 1
        monthly[month]['files_per_commit'].append(files)
    # Calculate avg files per commit
    for month in monthly:
        files_list = monthly[month]['files_per_commit']
        avg = sum(files_list) / len(files_list) if files_list else 0
        monthly[month]['avg_files_per_commit'] = round(avg, 2)
        del monthly[month]['files_per_commit']
    return monthly

def print_table(monthly, out=sys.stdout):
    header = [
        "Month", "Commits", "Files Changed", "LoC Added", "LoC Deleted", "Avg Files/Commit"
    ]
    print("{:<8} {:>8} {:>14} {:>10} {:>12} {:>18}".format(*header), file=out)
    for month in sorted(monthly.keys()):
        row = monthly[month]
        print("{:<8} {:>8} {:>14} {:>10} {:>12} {:>18}".format(
            month,
            row['commits'],
            row['files_changed'],
            row['insertions'],
            row['deletions'],
            row['avg_files_per_commit']
        ), file=out)

def write_csv(monthly, out_path):
    with open(out_path, "w", newline="") as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow([
            "Month", "Commits", "Files Changed", "LoC Added", "LoC Deleted", "Avg Files/Commit"
        ])
        for month in sorted(monthly.keys()):
            row = monthly[month]
            writer.writerow([
                month,
                row['commits'],
                row['files_changed'],
                row['insertions'],
                row['deletions'],
                row['avg_files_per_commit']
            ])

def print_summary(monthly, out=sys.stdout):
    """
    Print a summary table with totals for all months.
    """
    total_commits = sum(row['commits'] for row in monthly.values())
    total_files_changed = sum(row['files_changed'] for row in monthly.values())
    total_insertions = sum(row['insertions'] for row in monthly.values())
    total_deletions = sum(row['deletions'] for row in monthly.values())
    avg_files_per_commit = (
        total_files_changed / total_commits if total_commits else 0
    )
    print("\nSummary:", file=out)
    print("{:<8} {:>8} {:>14} {:>10} {:>12} {:>18}".format(
        "Total", total_commits, total_files_changed, total_insertions, total_deletions, round(avg_files_per_commit, 2)
    ), file=out)

def normalize_author(name, email):
    key = f"{name} <{email}>" if email else name
    return author_map.get(name, name)

def collect_authors_per_month(since="2024-10-01"):
    """
    Sammelt Commit-Autoren pro Monat.
    Gibt ein dict zurück: {monat: {author: commit_count, ...}, ...}
    """
    git_log_cmd = [
        "git", "log", f"--since={since}", "--pretty=format:%cd%x09%an%x09%ae", "--date=short"
    ]
    result = subprocess.run(git_log_cmd, capture_output=True, text=True, check=True)
    lines = result.stdout.splitlines()

    monthly_authors = defaultdict(lambda: defaultdict(int))
    total_authors = defaultdict(int)

    for line in lines:
        if not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        date = parts[0]
        name = parts[1]
        email = parts[2] if len(parts) > 2 else ""
        if name in author_blacklist or (email and email in author_blacklist):
            continue
        author = normalize_author(name, email)
        month = date[:7]
        monthly_authors[month][author] += 1
        total_authors[author] += 1

    return monthly_authors, total_authors

def print_authors_table(monthly_authors, out=sys.stdout):
    for month in sorted(monthly_authors.keys()):
        authors = monthly_authors[month]
        print(f"\n{month}:", file=out)
        print("{:<30} {:>8}".format("Author", "Commits"), file=out)
        sorted_authors = sorted(
            authors.items(),
            key=lambda x: x[1],
            reverse=True
        )
        for author, count in sorted_authors:
            if "<" in author and ">" in author:
                name, _ = author.rsplit(" <", 1)
            else:
                name = author
            print("{:<30} {:>8}".format(name, count), file=out)

def print_authors_summary(total_authors, out=sys.stdout):
    print("\nSummary (all months):", file=out)
    print("{:<30} {:>8}".format("Author", "Commits"), file=out)
    sorted_authors = sorted(
        total_authors.items(),
        key=lambda x: x[1],
        reverse=True
    )
    for author, count in sorted_authors:
        if "<" in author and ">" in author:
            name, _ = author.rsplit(" <", 1)
        else:
            name = author
        print("{:<30} {:>8}".format(name, count), file=out)

def write_authors_csv(monthly_authors, total_authors, out_path):
    with open(out_path, "w", newline="") as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(["Month", "Author", "Commits"])
        for month in sorted(monthly_authors.keys()):
            for author, count in sorted(monthly_authors[month].items(), key=lambda x: x[1], reverse=True):
                if "<" in author and ">" in author:
                    name, _ = author.rsplit(" <", 1)
                else:
                    name = author
                writer.writerow([month, name, count])
        # Summary
        writer.writerow([])
        writer.writerow(["Summary"])
        writer.writerow(["", "Author", "Commits"])
        for author, count in sorted(total_authors.items(), key=lambda x: x[1], reverse=True):
            if "<" in author and ">" in author:
                name, _ = author.rsplit(" <", 1)
            else:
                name = author
            writer.writerow(["", name, count])

def main():
    parser = argparse.ArgumentParser(
        description="Collect monthly git repository statistics (files changed, LoC added/deleted, avg files/commit, commits).",
        add_help=True
    )
    parser.add_argument("--csv", action="store_true", help="Output as CSV instead of table")
    parser.add_argument("--out", type=str, help="Write output to given file path")
    parser.add_argument("--since", type=str, default="2024-10-01", help="Start date (YYYY-MM-DD) for stats (default: 2024-10-01)")
    # argparse automatically adds --help
    args = parser.parse_args()

    stats = run_git_log(since=args.since)
    monthly = aggregate_monthly(stats)

    if args.csv:
        if args.out:
            write_csv(monthly, args.out)
        else:
            # Write to stdout as CSV
            writer = csv.writer(sys.stdout)
            writer.writerow([
                "Month", "Commits", "Files Changed", "LoC Added", "LoC Deleted", "Avg Files/Commit"
            ])
            for month in sorted(monthly.keys()):
                row = monthly[month]
                writer.writerow([
                    month,
                    row['commits'],
                    row['files_changed'],
                    row['insertions'],
                    row['deletions'],
                    row['avg_files_per_commit']
                ])
    else:
        if args.out:
            with open(args.out, "w") as f:
                print_table(monthly, out=f)
                print_summary(monthly, out=f)
        else:
            print_table(monthly)
            print_summary(monthly)

    monthly_authors, total_authors = collect_authors_per_month(args.since)
    if args.out and not args.csv:
        with open(args.out, "a") as f:
            print_authors_table(monthly_authors, out=f)
            print_authors_summary(total_authors, out=f)
    else:
        print_authors_table(monthly_authors)
        print_authors_summary(total_authors)

    if args.csv:
        if args.out:
            write_authors_csv(monthly_authors, total_authors, args.out)
        else:
            writer = csv.writer(sys.stdout)
            writer.writerow(["Month", "Author", "Email", "Commits"])
            for month in sorted(monthly_authors.keys()):
                for author, count in sorted(monthly_authors[month].items(), key=lambda x: x[1], reverse=True):
                    if "<" in author and ">" in author:
                        name, email = author.rsplit(" <", 1)
                        email = email.rstrip(">")
                    else:
                        name, email = author, ""
                    writer.writerow([month, name, email, count])
            writer.writerow([])
            writer.writerow(["Summary"])
            writer.writerow(["", "Author", "Email", "Commits"])
            for author, count in sorted(total_authors.items(), key=lambda x: x[1], reverse=True):
                if "<" in author and ">" in author:
                    name, email = author.rsplit(" <", 1)
                    email = email.rstrip(">")
                else:
                    name, email = author, ""
                writer.writerow(["", name, email, count])

if __name__ == "__main__":
    main()
