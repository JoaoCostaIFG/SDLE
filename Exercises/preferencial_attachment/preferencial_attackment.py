#!/usr/bin/env python3

from time import time
from random import choices

import networkx as nx
import matplotlib.pyplot as plt
import seaborn as sb
import pandas as pd


def rdmGraphCon(n):
    max_node = 2 ** n

    G = nx.Graph()
    G.add_node(0)

    srt_time = time()
    for i in range(1, max_node):
        nodes = [(node, G.degree(node) + 1) for node in G.nodes]
        nodeszip = list(zip(*nodes))
        node_to_con = choices(nodeszip[0], nodeszip[1])[0]

        G.add_node(i)
        G.add_edge(i, node_to_con)
    end_time = time()

    return (G, max_node, G.number_of_edges(), end_time - srt_time)


n = 8
G, max_node, edge_cnt, time = rdmGraphCon(n)

print(
    "For {} nodes, {} edges are needed. It finished in {} seconds.".format(
        max_node, edge_cnt, time
    )
)

#df = pd.DataFrame()
#for node in G.nodes():
#    df = df.append({"node": node, "degree": G.degree(node)}, ignore_index=True)
#df.sort_values("degree", ascending=False)[["degree"]].plot.bar(rot=0)

nx.draw(G, node_size=4)
plt.show()
