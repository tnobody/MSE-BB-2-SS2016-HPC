/**
 * Default naive implementation
**/
__kernel void scan(__global int* input, __global int* output, int n, __local int* temp) {
     int globalId = get_global_id(0);
     int localId = get_local_id(0);
     int pout = 0, pin = 1;
     
     temp[pout*n + globalId] = (localId > 0) ? input[globalId-1] : 0;
     barrier(CLK_LOCAL_MEM_FENCE || CLK_GLOBAL_MEM_FENCE);
     
     for (int offset = 1; offset < n; offset *= 2)
     {
          pout = 1 - pout;
          pin = 1 - pin;          
          if (globalId >= offset) {
               temp[pout*n+globalId] = temp[pin*n+globalId]+temp[pin*n+globalId - offset];
          } else {
               temp[pout*n+globalId] = temp[pin*n+globalId];
          }          
          barrier(CLK_LOCAL_MEM_FENCE || CLK_GLOBAL_MEM_FENCE);
     }
     output[globalId] = temp[pout*n+globalId];
}

__kernel void scan_work_efficient(
    __global int* input,
    __global int* output,
    const uint n,
    __local int* temp,
    __global int* temp_glob) {

     int localId = get_local_id(0);
     int globalId = get_global_id(0);
     int groupId = get_group_id(0);
     int offset = 1;

     // load input into shared memory
     temp[2*localId] = input[2*localId];
     temp[2*localId + 1] = input[2*localId + 1];

     // Build the sum in place up the tree
     for (int d = n>>1; d > 0; d >>=1) { 
          barrier(CLK_LOCAL_MEM_FENCE);

          if (localId < d) {
               int ai = offset * (2 * localId + 1) - 1;
               int bi = offset * (2 * localId + 2) - 1;

               temp[bi] += temp[ai];
          }
          offset *= 2;
     }

     // Save the last element of group in aux array + Clear the last element
     if (localId == 0) {
          temp_glob[groupId] = temp[n - 1];
          temp[n - 1] = 0;
     }

     // traverse down tree & build scan
     for (int d = 1; d < n; d *= 2) {
          offset >>= 1;
          barrier(CLK_LOCAL_MEM_FENCE);
          if (localId < d) {
               int ai = offset * (2 * localId + 1) - 1;
               int bi = offset * (2 * localId + 2) - 1;

               int t = temp[ai];
               temp[ai] = temp[bi];
               temp[bi] += t;
          }
     }

     barrier(CLK_LOCAL_MEM_FENCE);
     // write the results back to device memory
     output[2 * globalId] = temp[2*localId];
     output[2 * globalId + 1] = temp[2*localId + 1];
}

__kernel void add(__global int* input, __global int* output) {
     int globalId = get_global_id(0);
     int localId = get_local_id(0);
     int groupId = get_group_id(0);

     __local int value[1];

     if (localId == 0) {
          value[0] = input[groupId];
     }
     barrier(CLK_LOCAL_MEM_FENCE);

     output[2 * globalId] += value[0];
     output[2 * globalId + 1] += value[0];
}
