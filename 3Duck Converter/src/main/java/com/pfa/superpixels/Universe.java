/**
 * Dead code: is not used at all in the project
 */

package com.pfa.superpixels;

/**
 * @Deprecated
 */
@Deprecated
class UniElts
{
    int rank;
    int p;
    int size;
}

/**
 * @Deprecated
 */
@Deprecated
public class Universe
{
    UniElts elts[];
    private int num;


    Universe(int elements)
    {
        elts = new UniElts[elements];
        num = elements;
        for (int i = 0 ; i < elements ; i++) {
            elts[i].rank = 0;
            elts[i].size = 1;
            elts[i].p = i;
        }
    }


    int find(int x)
    {
        int y = x;
        while (y != elts[y].p) {
            y = elts[y].p;
        }
        elts[x].p = y;
        return y;
    }


    void join(int x, int y)
    {
        if (elts[x].rank > elts[y].rank) {
            elts[y].p = x;
            elts[x].size += elts[y].size;
        } else {
            elts[x].p = y;
            elts[y].size += elts[x].size;
            if (elts[x].rank == elts[y].rank) {
                elts[y].rank++;
            }
        }
        num--;
    }


    int size(int x)
    {
        return elts[x].size;
    }


    int numSets()
    {
        return num;
    }
}
