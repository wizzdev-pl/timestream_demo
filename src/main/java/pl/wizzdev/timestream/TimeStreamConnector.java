package pl.wizzdev.timestream;

import com.amazonaws.services.timestreamwrite.AmazonTimestreamWrite;
import com.amazonaws.services.timestreamwrite.AmazonTimestreamWriteClient;
import com.amazonaws.services.timestreamwrite.model.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TimeStreamConnector {

    @Value("{aws.write_service_endpoint}")
    private static final String WRITE_SERVICE_ENDPOINT = "ingest.timestream.us-east-1.amazonaws.com";
    @Value("{aws.singing_service_region}")
    private static final String SERVICE_SIGNING_REGION = "us-east-1";
    @Value("{h5data.databaseName}")
    private static final String DATABASE_NAME = "TimeStreamDemo";

    private static TimeStreamConnector instance;
    private static AmazonTimestreamWrite amazonTimestreamWrite = buildWriteClient();


    public Record setRecord (Dimension dimension, Measure measure) {
        Record record = new Record();
        record.setDimensions(Collections.singletonList(dimension));
        record.setMeasures(Collections.singletonList(measure));
        return record;
    }

    public Record setRecord (Collection<Dimension> dimension, Collection<Measure> measure) {
        Record record = new Record();
        record.setDimensions(dimension);
        record.setMeasures(measure);
        return record;
    }

    public WriteRecordsResult writeRecords(String tableName, Record record) {
        ArrayList<Record> records = new ArrayList<>();
        records.add(record);
        return this.sendRecords(tableName, records);
    }

    public WriteRecordsResult writeRecords(String tableName, List<Record> records) {
        return this.sendRecords(tableName, records);
    }

    public static TimeStreamConnector getInstance () {
        if (instance == null)
            instance = new TimeStreamConnector();
        return instance;
    }

    private WriteRecordsResult sendRecords(String tableName, Collection<Record> records) {
        WriteRecordsRequest writeRecordsRequest = new WriteRecordsRequest();
        writeRecordsRequest.setTimestampUnit("MILLISECONDS");
        writeRecordsRequest.withDatabaseName(DATABASE_NAME).withTableName(tableName).withRecords(records);
        return amazonTimestreamWrite.writeRecords(writeRecordsRequest);
    }

    private TimeStreamConnector () {}

    private static AwsClientBuilder.EndpointConfiguration getWriteEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(WRITE_SERVICE_ENDPOINT , SERVICE_SIGNING_REGION);
    }

    private static AmazonTimestreamWrite buildWriteClient() {
        return AmazonTimestreamWriteClient.builder().withEndpointConfiguration(getWriteEndpointConfiguration()).build();
    }


}