/*
 * Copyright 2017 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.web.vo.stat.chart;

import com.navercorp.pinpoint.common.server.bo.stat.DataSourceBo;
import com.navercorp.pinpoint.common.service.ServiceTypeRegistryService;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.web.mapper.stat.sampling.sampler.DataSourceSampler;
import com.navercorp.pinpoint.web.test.util.DataSourceTestUtils;
import com.navercorp.pinpoint.web.util.TimeWindow;
import com.navercorp.pinpoint.web.vo.Range;
import com.navercorp.pinpoint.web.vo.chart.Chart;
import com.navercorp.pinpoint.web.vo.chart.Point;
import com.navercorp.pinpoint.web.vo.stat.SampledDataSource;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Taejin Koo
 */
public class DataSourceChartGroupTest {

    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final int MIN_VALUE_OF_MAX_CONNECTION_SIZE = 20;
    private static final int CREATE_TEST_OBJECT_MAX_SIZE = 10;

    private final DataSourceSampler sampler = new DataSourceSampler();

    @Mock
    private ServiceTypeRegistryService serviceTypeRegistryService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(serviceTypeRegistryService.findServiceType(any(Short.class))).thenReturn(ServiceType.UNKNOWN);
    }

    @Test
    public void basicFunctionTest1() throws Exception {
        long currentTimeMillis = System.currentTimeMillis();
        TimeWindow timeWindow = new TimeWindow(new Range(currentTimeMillis - 300000, currentTimeMillis));

        List<SampledDataSource> sampledDataSourceList = createSampledDataSourceList(timeWindow);
        DataSourceChartGroup dataSourceChartGroup = new DataSourceChartGroup(timeWindow, sampledDataSourceList, serviceTypeRegistryService);

        assertEquals(sampledDataSourceList, dataSourceChartGroup);
    }

    @Test
    public void basicFunctionTest2() throws Exception {
        long currentTimeMillis = System.currentTimeMillis();
        TimeWindow timeWindow = new TimeWindow(new Range(currentTimeMillis - 300000, currentTimeMillis));

        List<SampledDataSource> sampledDataSourceList = Collections.emptyList();
        DataSourceChartGroup dataSourceChartGroup = new DataSourceChartGroup(timeWindow, sampledDataSourceList, serviceTypeRegistryService);

        Assert.assertEquals(-1, dataSourceChartGroup.getId());
        Assert.assertEquals(null, dataSourceChartGroup.getJdbcUrl());
        Assert.assertEquals(null, dataSourceChartGroup.getPoolName());
        Assert.assertEquals(null, dataSourceChartGroup.getServiceTypeName());
    }

    private List<SampledDataSource> createSampledDataSourceList(TimeWindow timeWindow) {
        List<SampledDataSource> sampledDataSourceList = new ArrayList<>();

        int maxConnectionSize = RANDOM.nextInt(MIN_VALUE_OF_MAX_CONNECTION_SIZE) + MIN_VALUE_OF_MAX_CONNECTION_SIZE;

        long from = timeWindow.getWindowRange().getFrom();
        long to = timeWindow.getWindowRange().getTo();

        for (long i = from; i < to; i += timeWindow.getWindowSlotSize()) {
            sampledDataSourceList.add(createSampledDataSource(i, maxConnectionSize));
        }

        return sampledDataSourceList;
    }

    private SampledDataSource createSampledDataSource(long timestamp, int maxConnectionSize) {
        int testObjectSize = RANDOM.nextInt(CREATE_TEST_OBJECT_MAX_SIZE) + 1;
        List<DataSourceBo> dataSourceBoList = DataSourceTestUtils.createDataSourceBoList(1, testObjectSize, maxConnectionSize);
        return sampler.sampleDataPoints(0, timestamp, dataSourceBoList, null);
    }

    private void assertEquals(List<SampledDataSource> sampledDataSourceList, DataSourceChartGroup dataSourceChartGroup) {
        Map<AgentStatChartGroup.ChartType, Chart> charts = dataSourceChartGroup.getCharts();

        Chart activeConnectionSizeChart = charts.get(DataSourceChartGroup.DataSourceChartType.ACTIVE_CONNECTION_SIZE);
        List<Point> activeConnectionSizeChartPointList = activeConnectionSizeChart.getPoints();

        for (int i = 0; i < sampledDataSourceList.size(); i++) {
            SampledDataSource sampledDataSource = sampledDataSourceList.get(i);
            Point<Long, Integer> point = sampledDataSource.getActiveConnectionSize();

            Assert.assertEquals(activeConnectionSizeChartPointList.get(i), point);
        }

        Chart maxConnectionSizeChart = charts.get(DataSourceChartGroup.DataSourceChartType.MAX_CONNECTION_SIZE);
        List<Point> maxConnectionSizeChartPointList = maxConnectionSizeChart.getPoints();
        for (int i = 0; i < sampledDataSourceList.size(); i++) {
            SampledDataSource sampledDataSource = sampledDataSourceList.get(i);
            Point<Long, Integer> point = sampledDataSource.getMaxConnectionSize();

            Assert.assertEquals(maxConnectionSizeChartPointList.get(i), point);
        }
    }

}
