package cz.muni.fi.modelchecker.mpi.termination;

import mpi.Comm;
import mpi.MPI;
import org.jetbrains.annotations.NotNull;

/**
 * Ensures token passing using MPI interface.
 */
public class MPITokenMessenger implements TokenMessenger {

    @NotNull
    private final Comm COMM;
    private final int TAG;

    public MPITokenMessenger(@NotNull Comm comm) {
        this(comm, -1);
    }
    public MPITokenMessenger(@NotNull Comm comm, int tag) {
        this.COMM = comm;
        this.TAG = tag;
    }

    @Override
    public int getProcessCount() {
        return COMM.Size();
    }

    @Override
    public int getMyId() {
        return COMM.Rank();
    }

    @Override
    public void sendTokenAsync(int destination, @NotNull Token token) {
        COMM.Isend(new int[]{token.flag, token.count}, 0, 2, MPI.INT, destination, TAG);
    }

    @NotNull
    @Override
    public Token waitForToken(int source) {
        @NotNull int[] buffer = new int[2];
        COMM.Recv(buffer, 0, buffer.length, MPI.INT, source, TAG);
        return new Token(buffer[0], buffer[1]);
    }
}
