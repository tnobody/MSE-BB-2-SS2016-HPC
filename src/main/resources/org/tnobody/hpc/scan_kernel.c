__kernel void scan(__global long* A, __global long* B, int C, __local long* temp) {
     int globalId = get_global_id(0);
     int localId = get_local_id(0);
     int pout = 0, pin = 1;

     //temp[thid] = A[thid];
     //B[thid] = temp[thid];

     temp[pout*C + globalId] = (localId > 0) ? A[globalId-1] : 0;
     barrier(CLK_LOCAL_MEM_FENCE || CLK_GLOBAL_MEM_FENCE);

     for (int offset = 1; offset < C; offset *= 2)
     {
          pout = 1 - pout;
          pin = 1 - pin;

          // mit local id arbeiten!!
          if (globalId >= offset) {
               temp[pout*C+globalId] = temp[pin*C+globalId]+temp[pin*C+globalId - offset];
          } else {
               temp[pout*C+globalId] = temp[pin*C+globalId];
          }

          //wenn localid == local_work_size - 1 => special array[get_group_id]

          barrier(CLK_LOCAL_MEM_FENCE || CLK_GLOBAL_MEM_FENCE);
     }

     B[globalId] = temp[pout*C+globalId];
     //B[thid] = (thid > 0) ? A[thid-1] : 0;
     //B[thid] = thid;
}