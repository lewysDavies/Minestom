package fr.themode.minestom.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockBatch implements BlockModifier {

    private static volatile ExecutorService batchesPool = Executors.newFixedThreadPool(2);

    private InstanceContainer instance;

    private Map<Chunk, List<BlockData>> data = new HashMap<>();

    public BlockBatch(InstanceContainer instance) {
        this.instance = instance;
    }

    @Override
    public synchronized void setBlock(int x, int y, int z, short blockId) {
        Chunk chunk = this.instance.getChunkAt(x, z);
        List<BlockData> blockData = this.data.getOrDefault(chunk, new ArrayList<>());

        BlockData data = new BlockData();
        data.x = x % 16;
        data.y = y;
        data.z = z % 16;
        data.blockId = blockId;

        blockData.add(data);

        this.data.put(chunk, blockData);
    }

    @Override
    public void setBlock(int x, int y, int z, String blockId) {
        Chunk chunk = this.instance.getChunkAt(x, z);
        List<BlockData> blockData = this.data.getOrDefault(chunk, new ArrayList<>());

        BlockData data = new BlockData();
        data.x = x % 16;
        data.y = y;
        data.z = z % 16;
        data.blockIdentifier = blockId;

        blockData.add(data);

        this.data.put(chunk, blockData);
    }

    public void flush() {
        for (Map.Entry<Chunk, List<BlockData>> entry : data.entrySet()) {
            Chunk chunk = entry.getKey();
            List<BlockData> dataList = entry.getValue();
            batchesPool.execute(() -> {
                synchronized (chunk) {
                    for (BlockData data : dataList) {
                        data.apply(chunk);
                    }
                    instance.sendChunkUpdate(chunk); // TODO partial chunk data
                }
            });
        }
    }

    private class BlockData {

        private int x, y, z;
        private short blockId;
        private String blockIdentifier;

        public void apply(Chunk chunk) {
            if (blockIdentifier == null) {
                chunk.setBlock((byte) x, (byte) y, (byte) z, blockId);
            } else {
                chunk.setCustomBlock((byte) x, (byte) y, (byte) z, blockIdentifier);
            }
        }

    }

}