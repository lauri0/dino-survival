import os

GAME_LOG_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "game_log.txt")
HUNTER_LOG_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "hunter_stats.yaml")


def _parse_simple_yaml(text: str) -> dict:
    data = {}
    stack = [(data, -1)]
    for raw in text.splitlines():
        if not raw.strip():
            continue
        indent = len(raw) - len(raw.lstrip())
        key_part = raw.strip()
        if key_part.endswith(":"):
            key = key_part[:-1]
            value = {}
        else:
            key, val = key_part.split(":", 1)
            val = val.strip()
            try:
                value = int(val)
            except ValueError:
                try:
                    value = float(val)
                except ValueError:
                    value = val
        while indent <= stack[-1][1]:
            stack.pop()
        parent = stack[-1][0]
        parent[key] = value
        if isinstance(value, dict):
            stack.append((value, indent))
    return data


def _dump_simple_yaml(data: dict, indent: int = 0) -> str:
    lines = []
    for k, v in data.items():
        if isinstance(v, dict):
            lines.append("  " * indent + f"{k}:")
            lines.append(_dump_simple_yaml(v, indent + 1))
        else:
            lines.append("  " * indent + f"{k}: {v}")
    return "\n".join(lines)


def load_hunter_stats() -> dict:
    if not os.path.exists(HUNTER_LOG_PATH):
        return {}
    with open(HUNTER_LOG_PATH) as f:
        text = f.read()
    return _parse_simple_yaml(text)


def save_hunter_stats(data: dict) -> None:
    text = _dump_simple_yaml(data)
    with open(HUNTER_LOG_PATH, "w") as f:
        f.write(text + "\n")


def append_game_log(formation: str, dino: str, turns: int, weight: float, won: bool) -> None:
    line = f"{formation}|{dino}|{turns}|{weight:.1f}|{'Win' if won else 'Loss'}\n"
    with open(GAME_LOG_PATH, "a") as f:
        f.write(line)


def update_hunter_log(formation: str, dino: str, hunts: dict) -> None:
    data = load_hunter_stats()
    form = data.setdefault(formation, {})
    dsection = form.setdefault(dino, {})
    for prey, (att, kill) in hunts.items():
        if kill > 0:
            dsection[prey] = dsection.get(prey, 0) + kill
    save_hunter_stats(data)
