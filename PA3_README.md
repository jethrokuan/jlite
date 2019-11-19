# Programming Assignment 3

To generate ARM instructions, the generator takes in the IR3 AST and performs several passes.

> *NOTE*: This meant that I had to revisit PA2, and fix everything that was there... If you'd consider regrading my PA2 based on this submission...

We will be following ARM's Calling Convention, the main rules we state below:

1. Registers R0-R3 are to contain the first four registers (caller's responsibility), and can be expected to be clobbered
2. Registers R4-R11 are callee-save registers, to be preserved by the function (callee's responsibility).
3. The stack we use is full-descending (we use instructions `STMFD` and `LDMFD`)

To manage the different passes, I create a `PassManager` at `jlite.pass.PassManager`, similar to how GCC manages their passes.

It runs the following passes in order:

1. `FlowPass`: This reads in the statements in each method (straight-line code), and constructs basic blocks and the control flow graph. We use the standard "leader-identification" algorithm described in the Dragon book.
2. `LowerPass`: ARM instructions have their own restrictions. For example, the first four arguments are to be in `R0 - R3`, and the return value to be in `R0`. This pass converts `Ir3` code into a format more amenable to ARM. Since this pass does not alter control flow, it can be done after `FlowPass`.
3. `LivePass`: This performs liveness analysis on the CFG, using the standard Dataflow Analysis setup. It first computes liveness at the basic block level (`liveIn` and `liveOut` per block), before using this information to compute `liveIn` and `liveOut` at the statement level. We can use liveness analysis for dead-code elimination as well as register allocation.
4. `RegAllocPass`: We use the liveness information to perform global register allocation via graph coloring.
    - First, we construct the `RegisterInterferenceGraph` by passing it `liveOut` information for every statement in the method. 
    - We compute a processing order for each node by using the heuristic of node degree.
    - We pre-color certain temporaries according to ARM constraints
        - call statement temporaries have to be `r0-r3` for the first 4 arguments
        - method arguments have to be `r0-r3` for the first 4 arguments
    - For each node in the processing order that is uncolored, attempt to assign a color. If it is unable to assign any of the 12 registers, spill the node to memory. Here we recompute liveness, reconstruct the register interference graph, and try again.
    - Nodes are spilled until a 12-coloring of the register interference graph is possible.
5. `ArmGenPass`: This pass takes the lowered `Ir3` code with registers allocated and generates ARM code.

## Optional: SSA Form
Initially, the plan was to perform optimizations on `Ir3` code in SSA form. SSA form makes def-use chains explicit, and facilitates some optimizations such as constant propagation, value range propagation and so on. However, deconstructing the SSA required additional care, and I did not have time to do so.

SSA with the minimal number of phi-functions was constructed by first computing dominance (`DominancePass`), via the standard dataflow analysis schema. Dominance Frontiers are computed from the dominance information, which is then used to construct the SSA (see [here](http://www.cs.cmu.edu/afs/cs/academic/class/15745-s12/public/lectures/L13-SSA-Concepts-1up.pdf)).
