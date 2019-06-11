package pl.wizzdev.timestream.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.amazonaws.services.timestreamwrite.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.wizzdev.timestream.model.H5File;
import pl.wizzdev.timestream.TimeStreamConnector;


@Service
public class H5Service {


    private final Logger logger = LogManager.getLogger(getClass());
    private static TimeStreamConnector timeStreamConnector = TimeStreamConnector.getInstance();

    @Value("{h5data.tableName}")
    private String tableName = "hdf5_demo";
    @Value("{h5data.batchWriteLimit}")
    private static final int batchWriteLimit = 10;
    @Value("{h5data.batchReadLimit}")
    private static final int batchReadLimit = 10;


    public void sendH5FileToTimeStream(String h5FilePath) {
        H5File h5File = new H5File(h5FilePath);
        h5File.readAttributes();

        long rowSize = h5File.getRowSize();
        long lastReadRow = 0;


        logger.info("Start reading data from file " + h5FilePath);

        while (rowSize > lastReadRow) {

            List<Record> records = new ArrayList<>();
            int limit = Math.min(batchReadLimit, (int) (rowSize - lastReadRow));
            short[][] readData = h5File.readPartDataset(lastReadRow, limit);

            for (int j = 0; j < Math.min(batchReadLimit, (int) (rowSize - lastReadRow)); j += batchWriteLimit) {

                short[][] data = new short[(int) h5File.getColumnSize()][batchWriteLimit];
                for (int i = 0; i < batchWriteLimit; i++) {
                    for (int ii = 0; ii < readData[i + j].length; ii++) {
                        data[ii][i] = readData[i + j][ii];
                    }
                }

                records.add(createRecord("hdf2", data));

                double percentage = ((100.0f * lastReadRow) / rowSize);
                logger.info("Status of reading sending data to TimeStream: " + percentage + "%");
            }

            lastReadRow += limit;
            timeStreamConnector.writeRecords(this.tableName, records);

        }

        logger.info("Sending records to TimeStream");
        h5File.closeFile();
    }

    private Record createRecord(String name, short[][] data) {

        List<Dimension> dimensions = new ArrayList<>();
        List<Measure> measures = new ArrayList<>();

        DimensionValue fileNameDimensionValue = new DimensionValue()
                .withValue(name)
                .withType(ValueType.STRING);
        Dimension fileNameDimension = new Dimension()
                .withName("hdf")
                .withValue(fileNameDimensionValue);
        dimensions.add(fileNameDimension);


        for (int ii = 0; ii < data.length; ii++) {
            List<MeasureValue> measureValues = new ArrayList<>();
            for (int i = 0; i < data[ii].length; i++) {
                long current_timestamp = System.currentTimeMillis() - data[ii].length;
                MeasureValue measureValue = new MeasureValue()
                        .withTimestamp(current_timestamp + i)
                        .withValue(Long.toString((long) data[ii][i]));
                measureValues.add(measureValue);
            }
            Measure measure = new Measure()
                    .withName("c" + ii)
                    .withType(ValueType.LONG)
                    .withValues(measureValues);
            measures.add(measure);
        }

        Record record = new Record();
        record.setDimensions(dimensions);
        record.setMeasures(measures);
        return record;
    }
}
