#include <netinet/in.h>
#include "sarc_ldp.h"

#define LINE_MAX 250

SARC_ExternalOperationInfo* SARC_tcp_get_socket_info(int sockfd)
{
  SARC_ExternalOperationInfo* info_out = NULL;
  for (SARC_uint32 i = 0; i < SARC_nb_external_socket; i++)
    {
      if (SARC_external_socket_out_info[i].fd == sockfd)
        {
           info_out = &SARC_external_socket_out_info[i];
           break;
        }
    }
  return info_out;
}

SARC_Ecode SARC_tcp_send_data(int sockfd, SARC_uint16 oper_id, SARC_char8* data, SARC_uint32 data_size)
{
  SARC_Ecode sarc_ecode = SARC_SUCCESS;
  SARC_ExternalOperationInfo* sarc_socket_info = NULL;
  int rb = 0;

  sarc_socket_info = SARC_tcp_get_socket_info(sockfd);

  if (sarc_socket_info != NULL)
    {
      if (sarc_socket_info->state == SARC_UNKNOWN || sarc_socket_info->state == SARC_NOT_CONNECTED) {
        rb = connect (sockfd, (struct sockaddr*) (&sarc_socket_info->addr), sizeof(struct sockaddr_in));
        if (rb < 0)
          {
            if(errno == EISCONN)
              {
                sarc_socket_info->state = SARC_CONNECTED;
              }
            else
              {
                if (sarc_socket_info->state == SARC_UNKNOWN)
                  {
                    perror ("tcp_send_data");
                    sarc_socket_info->state = SARC_NOT_CONNECTED;
                  }
                sarc_ecode = SARC_FAILURE;
              }
          }
        else
          {
             sarc_socket_info->state = SARC_CONNECTED;
          }
      }

      if(sarc_socket_info->state == SARC_CONNECTED)
        {
          SARC_SerializationContext sarc_serial_ctxt;
          SARC_uint16 dummy = 0;
          SARC_uint32 message_size = 0;
          SARC_uint16 external_id = 0;
          SARC_uint32 payload_size = 0;
          SARC_uint32 err = 0;

          SARC_char8 SARC_routing_buffer_header[SARC_EXTERNAL_ROUTE_HEADER_MESSAGE_SIZE];

          SARC_serial_start_serialize (&sarc_serial_ctxt, &SARC_routing_buffer_header, SARC_FALSE);
          message_size = htonl(data_size + SARC_EXTERNAL_ROUTE_HEADER_SIZE);
          SARC_uint32_serialize(&sarc_serial_ctxt, &message_size);
          SARC_uint16_serialize(&sarc_serial_ctxt, &dummy);
          external_id = htons(oper_id);
          SARC_uint16_serialize (&sarc_serial_ctxt, &external_id);
          payload_size = htonl(data_size);
          SARC_uint32_serialize(&sarc_serial_ctxt, &payload_size);

          err = send (sockfd, SARC_routing_buffer_header, SARC_EXTERNAL_ROUTE_HEADER_MESSAGE_SIZE, 0);

          if (err <= 0)
            {
              perror ("Error on send header for extern operation");
              if (errno == EAGAIN )
                {
                  abort();
                }
              sarc_ecode = SARC_FAILURE;
            }

          err = send (sockfd, data, data_size, 0);

          if (err <= 0)
            {
              perror ("Error on send payload for extern operation");
              if (errno == EAGAIN )
                {
                  abort();
                }
              sarc_ecode = SARC_FAILURE;
            }

        }
    }
  return sarc_ecode;
}

SARC_Ecode SARC_get_port_info(const char* search_port_name,
                  char* address_out, int* port_out)
{
    FILE *f = fopen("external_conf.txt", "r");
    if (!f) {
        perror("Error opening file");
        return 0;
    }

    char line[LINE_MAX];
    char port_name[64], address[64];
    int port;
    SARC_Ecode found = SARC_FAILURE;

    while (fgets(line, LINE_MAX, f) != NULL) {
        line[strcspn(line, "\r\n")] = 0;
        if (sscanf(line, "%63s %63s %d", port_name, address, &port) == 3) {
            if (strcmp(port_name, search_port_name) == 0) {
                strncpy(address_out, address, 16);
                address_out[16 - 1] = '\0'; // Sécurité
                *port_out = port;
                found = SARC_SUCCESS;
                break;
            }
        }
    }
    fclose(f);
    return found;
}
