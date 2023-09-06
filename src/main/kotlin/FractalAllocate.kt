import java.io.File
import kotlin.random.Random

abstract class Allocator {
    protected val data = mutableListOf<Int>()
    protected val memory = mutableListOf<Boolean>()
    protected val allocRecord = mutableMapOf<Int, Int>()
    val highWaterMark get() = memory.size
    abstract fun alloc(n: Int): Int
    fun free(p: Int) {
        if(!allocRecord.containsKey(p))
            throw IllegalStateException("Freeing unallocated memory")
        val n = allocRecord[p]!!
        for(i in 0 until n) {
            val idx = accessIdx(p, i)
            memory[idx] = false
            data[idx] = 0
        }
        allocRecord.remove(p)
    }
    abstract fun accessIdx(p: Int, off: Int): Int
    fun get(p : Int, off: Int) : Int {
        val idx = accessIdx(p, off)
        if(idx >= memory.size)
            throw IllegalStateException("Access out of bound")
        if(!memory[idx])
            throw IllegalStateException("Accessing unallocated memory")
        return data[idx]
    }
    fun set(p : Int, off: Int,  v : Int) {
        val idx = accessIdx(p, off)
        if(idx >= memory.size)
            throw IllegalStateException("Access out of bound")
        if(!memory[idx])
            throw IllegalStateException("Accessing unallocated memory")
        data[idx] = v
    }
    fun size(p : Int) = allocRecord[p]!!
}

class SerialAllocator : Allocator() {
    override fun alloc(n: Int): Int {
        var lastAlloc = -1
        for(i in memory.indices) {
            if(memory[i]) {
                lastAlloc = i
                continue
            }
            if(i - lastAlloc >= n) {
                for(j in lastAlloc + 1 .. i) {
                    memory[j] = true
                }
                allocRecord[lastAlloc + 1] = n
                return lastAlloc + 1
            }
        }
        for(i in lastAlloc + 1 until memory.size) {
            memory[i] = true
        }
        val newSize = n - (memory.size - lastAlloc - 1)
        data.addAll(Array(newSize) { 0 })
        memory.addAll(Array(newSize) { true })
        allocRecord[lastAlloc + 1] = n
        return lastAlloc + 1
    }

    override fun accessIdx(p: Int, off: Int) = p + off
}

class FractalAllocator : Allocator() {
    private val fractalCache = mutableListOf<Int>()
    override fun alloc(n: Int): Int {
        var i = 0
        while(true) {
            var j = 0
            while(j < n) {
                if(accessOrCreate(accessIdx(i, j))) break
                j++
            }
            if(j == n) {
                for(k in 0 until n) {
                    memory[accessIdx(i, k)] = true
                }
                allocRecord[i] = n
                return i
            }
            i++
        }
    }

    private fun log2i(n : Int) : Int {
        var result = 0
        var i = n
        while(i > 1) {
            i = i shr 1
            result++
        }
        return result
    }

    private fun pow3i(n : Int) : Int {
        var result = 1
        repeat(n) {
            result *= 3
        }
        return result
    }

    // maybe there is a better way to compute fractals
    private fun computeNextFractal() {
        if(fractalCache.size == 0) {
            fractalCache.add(0)
            return
        }
        val idx = fractalCache.size - 1
        val neg = idx.inv()
        val zpospow2 = neg xor (neg and (neg - 1))
        val zpos = log2i(zpospow2) // find the position of the most significant '0' in the binary representation of idx
        val addend = pow3i(zpos)
        fractalCache.add(fractalCache.last() + addend + 1)
    }
    override fun accessIdx(p: Int, off: Int): Int {
        if(fractalCache.size <= off)
            repeat(off - fractalCache.size + 1) { computeNextFractal() }
        return fractalCache[off] + p
    }
    private fun accessOrCreate(mem: Int) : Boolean {
        if(memory.size <= mem) {
            val newSize = mem - memory.size + 1
            memory.addAll(Array(newSize) { false })
            data.addAll(Array(newSize) { 0 })
        }
        return memory[mem]
    }
}

fun main() {
    val fractal = FractalAllocator()
    val serial = SerialAllocator()
    val fractalMem = mutableListOf<Int>()
    val serialMem = mutableListOf<Int>()
    val csv = File("allocate.csv").bufferedWriter()
    csv.write("Action,Size,Holding,pf,ps,Fractal,Serial\n")
    var holdingMemory = 0
    repeat(1000) {
        var n = Random.nextInt(if(it < 200) 1 else 10, 20)
        val doFree =
            if(it < 200) false else Random.nextBoolean()
                && fractalMem.size > 0
        if(doFree) {
            val idx = Random.nextInt(0, fractalMem.size)
            val pf = fractalMem[idx]
            val ps = serialMem[idx]
            n = serial.size(ps)
            holdingMemory -= n
            for(i in 0 until n) {
                if(fractal.get(pf, i) != pf)
                    throw IllegalStateException("Fractal: Memory corrupted")
                if(serial.get(ps, i) != ps)
                    throw IllegalStateException("Serial: Memory corrupted")
            }
            fractal.free(pf)
            serial.free(ps)
            fractalMem.removeAt(idx)
            serialMem.removeAt(idx)
            csv.write("Free,$n,$holdingMemory,$pf,$ps,${fractal.highWaterMark},${serial.highWaterMark}\n")
        } else {
            holdingMemory += n
            val pf = fractal.alloc(n)
            val ps = serial.alloc(n)
            for(i in 0 until n) {
                fractal.set(pf, i, pf)
                serial.set(ps, i, ps)
            }
            fractalMem.add(pf)
            serialMem.add(ps)
            csv.write("Alloc,$n,$holdingMemory,$pf,$ps,${fractal.highWaterMark},${serial.highWaterMark}\n")
        }
    }
    csv.flush()
    csv.close()
}