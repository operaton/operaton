#!/usr/bin/env python3

import argparse
import datetime
import os
import sys
import requests
from collections import defaultdict

GITHUB_API = "https://api.github.com"
REPO = "operaton/operaton"
DEFAULT_START = datetime.date(2024, 10, 1)

def get_github_token():
    return os.environ.get("GITHUB_TOKEN")

def github_api(url, params=None):
    headers = {}
    token = get_github_token()
    if token:
        headers["Authorization"] = f"token {token}"
    resp = requests.get(url, headers=headers, params=params)
    resp.raise_for_status()
    return resp.json()

def get_contributor_history(start_date):
    # Get all commits since start_date
    contributors_by_month = defaultdict(set)
    page = 1
    per_page = 100
    while True:
        params = {
            "since": start_date.isoformat(),
            "per_page": per_page,
            "page": page
        }
        url = f"{GITHUB_API}/repos/{REPO}/commits"
        commits = github_api(url, params)
        if not commits:
            break
        for commit in commits:
            author = commit.get("author")
            if author:
                login = author["login"]
                date_str = commit["commit"]["author"]["date"]
                date = datetime.datetime.strptime(date_str, "%Y-%m-%dT%H:%M:%SZ").date()
                month = date.replace(day=1)
                contributors_by_month[month].add(login)
        if len(commits) < per_page:
            break
        page += 1
    return contributors_by_month

def get_stars_history(start_date):
    # GitHub API does not provide star history directly, but we can get stargazers with timestamps
    stars_by_month = defaultdict(int)
    page = 1
    per_page = 100
    while True:
        url = f"{GITHUB_API}/repos/{REPO}/stargazers"
        params = {"per_page": per_page, "page": page}
        headers = {"Accept": "application/vnd.github.v3.star+json"}
        token = get_github_token()
        if token:
            headers["Authorization"] = f"token {token}"
        resp = requests.get(url, headers=headers, params=params)
        resp.raise_for_status()
        stargazers = resp.json()
        if not stargazers:
            break
        for star in stargazers:
            starred_at = star.get("starred_at")
            if starred_at:
                date = datetime.datetime.strptime(starred_at, "%Y-%m-%dT%H:%M:%SZ").date()
                if date >= start_date:
                    month = date.replace(day=1)
                    stars_by_month[month] += 1
        if len(stargazers) < per_page:
            break
        page += 1
    # Accumulate stars per month
    months = sorted(stars_by_month.keys())
    acc = 0
    stars_accum = {}
    for m in months:
        acc += stars_by_month[m]
        stars_accum[m] = acc
    return stars_accum

def render_svg_single(months, values, ylabel, color, title, target_path):
    width = 800
    height = 400
    margin = 60
    n_months = len(months)
    max_val = max(values.values()) if values else 1

    def x_pos(i):
        return margin + i * (width - 2 * margin) // max(1, n_months - 1)

    def y_pos(val):
        return height - margin - int((val / max_val) * (height - 2 * margin))

    svg = [
        f'<svg width="{width}" height="{height}" xmlns="http://www.w3.org/2000/svg">',
        f'<rect width="100%" height="100%" fill="#fff"/>',
        f'<text x="{width//2}" y="30" text-anchor="middle" font-size="20">{title}</text>',
        f'<text x="{margin}" y="{height-margin+30}" font-size="14">Months (from {months[0].strftime("%b %Y")})</text>',
        f'<text x="10" y="{margin}" font-size="14" fill="{color}">{ylabel}</text>',
        # Axes
        f'<line x1="{margin}" y1="{height-margin}" x2="{width-margin}" y2="{height-margin}" stroke="black"/>',
        f'<line x1="{margin}" y1="{margin}" x2="{margin}" y2="{height-margin}" stroke="black"/>',
    ]
    # X labels
    for i, m in enumerate(months):
        x = x_pos(i)
        svg.append(f'<text x="{x}" y="{height-margin+15}" font-size="12" text-anchor="middle">{m.strftime("%b %Y")}</text>')
    # Y labels
    for yv in range(0, max_val+1, max(1, (max_val//10))):
        y = y_pos(yv)
        svg.append(f'<text x="{margin-10}" y="{y+5}" font-size="12" text-anchor="end">{yv}</text>')
        svg.append(f'<line x1="{margin-5}" y1="{y}" x2="{width-margin}" y2="{y}" stroke="#eee"/>')
    # Value line
    points = " ".join(f"{x_pos(i)},{y_pos(values.get(m,0))}" for i, m in enumerate(months))
    svg.append(f'<polyline points="{points}" fill="none" stroke="{color}" stroke-width="2"/>')
    svg.append('</svg>')
    with open(target_path, "w") as f:
        f.write("\n".join(svg))

def main():
    parser = argparse.ArgumentParser(description="Render Operaton repo history as SVG")
    parser.add_argument("--start", type=str, default="2024-10", help="Start month (YYYY-MM)")
    parser.add_argument("--output-dir", type=str, default=".", help="Output directory for SVG")
    args = parser.parse_args()

    try:
        start_date = datetime.datetime.strptime(args.start, "%Y-%m").date()
    except Exception:
        print("Invalid start date format. Use YYYY-MM.")
        sys.exit(1)

    print(f"Fetching contributor history since {start_date}...")
    contributors_by_month = get_contributor_history(start_date)
    print(f"Fetching stars history since {start_date}...")
    stars_by_month = get_stars_history(start_date)

    # Build month list
    all_months = set(contributors_by_month.keys()) | set(stars_by_month.keys())
    if not all_months:
        print("No data found for the given period.")
        sys.exit(1)
    months = sorted(all_months)
    contributors = {m: len(contributors_by_month.get(m, set())) for m in months}
    stars = {m: stars_by_month.get(m, 0) for m in months}

    # Calculate total contributors per month (cumulative)
    total_contributors_set = set()
    total_contributors = {}
    for m in months:
        total_contributors_set.update(contributors_by_month.get(m, set()))
        total_contributors[m] = len(total_contributors_set)

    contrib_svg = os.path.join(args.output_dir, "repo-contributors.svg")
    stars_svg = os.path.join(args.output_dir, "repo-stars.svg")
    total_contrib_svg = os.path.join(args.output_dir, "repo-total-contributors.svg")

    print(f"Rendering contributors SVG to {contrib_svg}...")
    render_svg_single(months, contributors, "Contributors", "blue", "Operaton Contributors History", contrib_svg)
    print(f"Rendering stars SVG to {stars_svg}...")
    render_svg_single(months, stars, "GitHub Stars", "orange", "Operaton GitHub Stars History", stars_svg)
    print(f"Rendering total contributors SVG to {total_contrib_svg}...")
    render_svg_single(months, total_contributors, "Total Contributors", "green", "Operaton Total Contributors History", total_contrib_svg)
    print("Done.")

if __name__ == "__main__":
    main()
