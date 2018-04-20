# SynGHS
Minimum spanning tree calculation algorithm for Distributed Synchronous Networks (GHS)

We implemented this algorithm using simple synchronizer. We use notion of rounds and phases and buffer out of phase messages.
This guarantees that none of the node goes too ahead or left too behind.

Message passing mechanism used in this algorithm is simple socket programming (Peer to Peer) which allows one hop communication. This is identical to readl time distributed network.

For more info on GHS algorithm please visit
https://en.wikipedia.org/wiki/Distributed_minimum_spanning_tree

Your comments and suggestions are welcome. Please mail me at nileshpharate94@gmail.com
