# Fractal Allocator

> A failed attempt.

Fragmentation is a common problem in memory allocation.
And there are multiple existing attempts addressing this problem, 
such as compacting and paging. This repository tries to solve the problem in a fundamentally different way, 
albeit failed in the end.

Considering filling a big square with differently sized small squares, 
it's harder to utilize all the spaces, compared with allocating linear memories.
This raises a question: is it true that space allocation is easier in lesser dimensions?
In integer dimensions up to 3D, that's probably true, since it seems even harder 
to fill a box with cubes. Unfortunately, there is no space in 0 dimension, and we don't
have other dimensions between 0D and 1D. Or... do we?

![squares](/assets/squares.png)

In fractal field, there exist non-integer dimensions. And yes, there is a fractal shape
whose dimension number is between 0 and 1: the Cantor set.

![cantor](/assets/cantor.jpg)

With a dimensionality of 0.631, the Cantor set could be an inspiration 
for a brand-new memory allocator! Instead of allocating continuous memory, 
the allocator splits the requested allocation size into fixed-sized chunks, 
which then are arranged in the shape of the Cantor set.

![alloc](/assets/alloc.png)

Since there are unused spaces between allocated chunks, the allocator allows allocated
memory interleave with each other.

Although sounds very innovative, the allocator actually does pretty bad,
compared with a baseline allocator that allocates memory serially. In the test,
we first allocate 200 memory regions with random sizes, then allocate and free 
memory regions randomly 800 times. Feeding both allocator with the same data,
the results are as follows:

![memory cap](/assets/cap.png)
![memory utilization](/assets/util.png)

Maybe fractal allocators are not a good idea in the beginning...