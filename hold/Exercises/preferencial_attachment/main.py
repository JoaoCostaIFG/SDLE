#!/usr/bin/env python3

import networkx as nx
from time import time
import matplotlib.pyplot as plt
import pandas as pd
from random import choices


def rdmGraphCon(n, N):
    G = nx.Graph()
    max_node = 2 ** n
    edge_cnt = []
    times = []
    for _ in range(N):
        G.clear()
        G.add_nodes_from([i for i in range(max_node)])

        srt_time = time()
        while not nx.is_connected(G):
            nodes = [(node, G.degree(node) + 1) for node in G.nodes]
            nodeszip = list(zip(*nodes))
            node1 = choices(nodeszip[0], nodeszip[1])[0]

            non_neighbors = list(nx.non_neighbors(G, node1))

            remaining_choices = list(filter(lambda x: x[0] in non_neighbors, nodes))
            remainingzip = list(zip(*remaining_choices))
            node2 = choices(remainingzip[0], remainingzip[1])[0]

            G.add_edge(node1, node2)
        end_time = time()

        edge_cnt.append(G.number_of_edges())
        times.append(end_time - srt_time)

    return (G, max_node, edge_cnt, times)


N = 30
max_n = 6
df = pd.DataFrame()
for n in range(1, max_n + 1):
    G, max_node, edge_cnt, times = rdmGraphCon(n, N)
    mean_edge = sum(edge_cnt) / N
    print(
        "For {} nodes, {} edges are needed: min {}, max {}.".format(
            max_node, mean_edge, min(edge_cnt), max(edge_cnt)
        )
    )
    for i in range(N):
        df = df.append(
            {"nodes": max_node, "edges": edge_cnt[i], "time": times[i]},
            ignore_index=True,
        )

    if n == max_n:
        nx.draw(G)

fig = plt.figure()
ax = fig.add_subplot(111, projection="3d")
x = df["nodes"]
ax.set_xlabel("Number of nodes")
y = df["edges"]
ax.set_ylabel("Number of edges")
z = df["time"]
ax.set_zlabel("Time elapsed")
ax.scatter(x, y, z)
# ax.plot_trisurf(x, y, z, linewidth=5, cmap=plt.cm.Spectral)
plt.show()
