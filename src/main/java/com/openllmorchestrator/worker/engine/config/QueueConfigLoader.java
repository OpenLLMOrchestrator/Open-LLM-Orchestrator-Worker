package com.openllmorchestrator.worker.engine.config;

public class QueueConfigLoader {

    private final RedisConfigRepository redisRepo;
    private final DbConfigRepository dbRepo;
    private final FileConfigRepository fileRepo;

    public QueueConfigLoader(
            RedisConfigRepository redisRepo,
            DbConfigRepository dbRepo,
            FileConfigRepository fileRepo) {

        this.redisRepo = redisRepo;
        this.dbRepo = dbRepo;
        this.fileRepo = fileRepo;
    }

    public QueueConfig load(String queueName) {

        // 1️⃣ Redis
        QueueConfig config =
                redisRepo.find(queueName);

        if (config != null) {
            return config;
        }

        // 2️⃣ DB
        config = dbRepo.find(queueName);

        if (config != null) {
            redisRepo.save(config);
            return config;
        }

        // 3️⃣ File
        config = fileRepo.find(queueName);

        if (config != null) {
            dbRepo.save(config);
            redisRepo.save(config);
            return config;
        }

        throw new IllegalStateException(
                "No configuration found for queue: "
                        + queueName);
    }
}
