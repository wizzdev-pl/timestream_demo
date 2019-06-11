package pl.wizzdev.timestream.model;

import hdf.hdf5lib.HDFNativeData;
import hdf.hdf5lib.HDF5GroupInfo;
import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
//import hdf.object.h5.H5Group;
import org.apache.commons.lang.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.wizzdev.timestream.exceptions.H5FileEndException;
import pl.wizzdev.timestream.exceptions.H5FileException;

import java.lang.reflect.Array;
import java.util.*;


public class H5File {

    private final Logger logger = LogManager.getLogger(getClass());
    private static int dataDimension = 2;

    private double startTime;
    private double samplingInterval;

    private String pathToFile;
    private boolean ready = false;
    private long fileHook;
    private long groupHook;
    private long datasetHook;
    private long columnSize;
    private long rowSize;

    public void readAttributes() {
        if (!this.isReady()) {
            logger.error("Trying to read attributes from not ready file");
            throw new H5FileException();
        }

        long[] dims = {1};

        double[] startTime = {0};
        double[] samplingInterval = {0};

        logger.debug("Trying to read start time attribute from dataset");
        long idStartTimeAttribute = H5.H5Aopen_by_name(
                this.datasetHook, ".", "start_time", HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        long idStartTimeAttributeDataspace = H5.H5Aget_space(idStartTimeAttribute);
        H5.H5Sget_simple_extent_dims(idStartTimeAttributeDataspace, dims, null);
        H5.H5Aread(idStartTimeAttribute, HDF5Constants.H5T_NATIVE_DOUBLE, startTime);
        this.startTime = startTime[0];


        logger.debug("Trying to read sampling interval attribute from dataset");
        long idSamplingIntervalAttribute = H5.H5Aopen_by_name(
                this.datasetHook, ".", "sampling_interval", HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        long idSamplingIntervalAttributeDataspace = H5.H5Aget_space(idSamplingIntervalAttribute);
        H5.H5Sget_simple_extent_dims(idSamplingIntervalAttributeDataspace, dims, null);
        H5.H5Aread(idSamplingIntervalAttribute, HDF5Constants.H5T_NATIVE_DOUBLE, samplingInterval);
        this.samplingInterval = samplingInterval[0];
    }

    public void closeFile() {
        H5.H5Dclose(this.datasetHook);
        H5.H5Gclose(this.groupHook);
        H5.H5Fclose(this.fileHook);
    }

    public long getRowSize() {
        return this.rowSize;
    }

    public long getColumnSize() {
        return this.columnSize;
    }

    public short[][] readPartDataset(long start, int limit) {
        logger.debug("Start reading dataset");

        short[][] datasetData = new short[limit][(int)this.columnSize];

        if (!this.isReady()) {
            logger.error("Trying to read not ready file");
            throw new H5FileException();
        } else if (this.rowSize < start) {
            logger.error("End of file occurred");
            throw new H5FileEndException();
        } else {

            long fileSpaceId;
            long memSpaceId;

            long[] start_position = {start, 0};
            long[] stride = {1, 1};
            long[] count = {limit, (int) this.columnSize};
            long[] block = null;

            try {
                memSpaceId = H5.H5Screate_simple(2, count, null);
                fileSpaceId = H5.H5Dget_space(this.datasetHook);
                H5.H5Sselect_hyperslab(fileSpaceId, HDF5Constants.H5S_SELECT_SET, start_position, stride, count, block);

                H5.H5Dread(this.datasetHook,
                        HDF5Constants.H5T_NATIVE_SHORT,
                        memSpaceId,
                        fileSpaceId,
                        HDF5Constants.H5P_DEFAULT,
                        datasetData
                );

            } catch (Exception exception) {
                logger.error("Error occurred while reading dataset. ");
                exception.printStackTrace();
            }

            return datasetData;
        }
    }

    private Boolean isReady() {
        this.ready = (this.fileHook > 0 && this.groupHook > 0 && this.datasetHook > 0);
        return this.ready;
    }

    private long openFile() {
        try {
            logger.debug("Trying to open H5 file - " + this.pathToFile);
            return H5.H5Fopen(this.pathToFile, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);
        } catch (Exception exception) {
            logger.error("Couldn't open H5 file");
            exception.printStackTrace();
            return -1;
        }
    }

    private long openGroup() {
        try {
            logger.debug("Trying to open group 'data' of H5 file - " + this.pathToFile);
            return H5.H5Gopen(this.fileHook, "/data", HDF5Constants.H5P_DEFAULT);
        } catch (Exception exception) {
            logger.error("Couldn't open H5 group");
            exception.printStackTrace();
            return -1;
        }
    }

    private long openDataset() {
        try {
            logger.debug("Trying to open dataset 'dataset' of H5 file - " + this.pathToFile);
            return H5.H5Dopen(this.groupHook, "data_set", HDF5Constants.H5P_DEFAULT);
        } catch (Exception exception) {
            logger.error("Couldn't open H5 dataset");
            exception.printStackTrace();
            return -1;
        }
    }

    private void checkSize() {
        long datasetSpace = H5.H5Dget_space(this.datasetHook);
        long datasetDimsCount = H5.H5Sget_simple_extent_ndims(datasetSpace);
        long[] datasetDims = new long[(int) datasetDimsCount];
        H5.H5Sget_simple_extent_dims(datasetSpace, datasetDims, null);

        this.rowSize = datasetDims[0];
        this.columnSize = datasetDims[1];

        logger.debug("Reading dataset size");
        logger.debug("Rows count: " + this.rowSize);
        logger.debug("Columns count: " + this.columnSize);
    }

    public H5File(String pathToFile) {
        this.pathToFile = pathToFile;
        this.fileHook = this.openFile();
        this.groupHook = this.openGroup();
        this.datasetHook = this.openDataset();

        this.checkSize();

        if (this.isReady()) logger.info("File is read and ready - " + this.pathToFile);

    }
}
