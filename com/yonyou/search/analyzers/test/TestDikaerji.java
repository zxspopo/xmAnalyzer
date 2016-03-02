package com.yonyou.search.analyzers.test;

import com.yonyou.search.analyzers.utils.CartesianProductUtils;

import java.util.Arrays;

/**
 * Created by zengxs on 2016/2/25.
 */
public class TestDikaerji {

    public static void main(String[] args) throws Exception {
        String[] aa = {"aa1", "aa2"};
        String[] bb = {"bb1", "bb2", "bb3"};
        String[] cc = {"cc1", "cc2", "cc3", "cc4"};
        String[] dd = {"dd1", "dd2", "dd3", "dd4"};
        String[][] xyz = {aa, bb, cc, dd};
        System.out.println(Arrays.toString(CartesianProductUtils.calc(xyz)));
    }

}

