/**
 * Dead code: is not used at all in the project
 */

package com.pfa.superpixels;

import java.util.Arrays;
import java.util.Comparator;


/**
 * @Deprecated
 */
@Deprecated
public class SegmentGraph
{
    static Universe segmentGraph(int numVertices, int numEdges, Edge edges[], float c)
    {
        Comparator<Edge> compareEdges = new Comparator<Edge>() {
            @Override
            public int compare(Edge a, Edge b) {
                if (a.w < b.w) {
                    return 1;
                } else if (a.w > b.w) {
                    return -1;
                }
                return 0;
            }
        };

        Arrays.sort(edges, 0, numEdges, compareEdges);

        Universe u = new Universe(numVertices);


        float threshold[] = new float[numVertices];
        for (int i = 0 ; i < numVertices ; i++) {
            threshold[i] = c / 1;
        }


        for (int i = 0 ; i < numEdges ; i++) {
            Edge pedge = edges[i];

            int a = u.find(pedge.a);
            int b = u.find(pedge.b);
            if (a != b) {
                if ((pedge.w <= threshold[a]) &&
                        (pedge.w <= threshold[b])) {
                    u.join(a, b);
                    a = u.find(a);
                    threshold[a] = pedge.w + c / u.size(a);
                }
            }
        }

        return u;
    }
}
