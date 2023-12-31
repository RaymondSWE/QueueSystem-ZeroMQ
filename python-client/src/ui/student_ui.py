import tkinter as tk
from tkinter import ttk, Listbox, PhotoImage
from ttkthemes import ThemedTk
from utils.student_logic import QueueLogic


class QueueUI(ThemedTk):
    def __init__(self):
        super().__init__()

        self.set_theme("equilux")
        self.title("Student Client")
        self.geometry("900x600")

        self.user_icon = PhotoImage(file='assets/user.png').subsample(20, 20)
        self.connect_icon = PhotoImage(file='assets/server-connection.png').subsample(15, 15)
        self.join_icon = PhotoImage(file='assets/line.png').subsample(15, 15)
        self.server_icon = PhotoImage(file='assets/server.png').subsample(20, 20)
        self.successful_connection_icon = PhotoImage(file='assets/successful_connection.png').subsample(20, 20)
        self.failed_connection_icon = PhotoImage(file='assets/failed_connection.png').subsample(20, 20)

        self.style = ttk.Style()
        self.style.configure('.', padding=5, foreground='#ffffff')

        # Main frame
        content_frame = ttk.Frame(self)
        content_frame.pack(fill=tk.BOTH, expand=True)

        # Different input
        input_section = ttk.Frame(content_frame)
        input_section.grid(row=0, column=0, columnspan=4, sticky="ew", pady=20)

        # Name input
        ttk.Label(input_section, text="Name:", image=self.user_icon, compound="left", font=("Poppins", 14)).grid(row=0,
                                                                                                                 column=0,
                                                                                                                 padx=10,
                                                                                                                 pady=10)
        self.name_entry = ttk.Entry(input_section, font=("Poppins", 12), width=30)
        self.name_entry.grid(row=0, column=1, padx=10, pady=10)

        # Connect button
        self.connect_button = ttk.Button(input_section, text="Connect to server", image=self.connect_icon,
                                         compound="left", command=self.connect_to_server)
        self.connect_button.grid(row=0, column=2, padx=(10, 20), pady=10)

        # Join queue button
        self.join_queue_button = ttk.Button(input_section, text="Join Queue", image=self.join_icon, compound="left",
                                            state=tk.DISABLED)
        self.join_queue_button.grid(row=0, column=3, padx=10, pady=10)

        # Host and Port Input section
        host_port_section = ttk.Frame(content_frame)
        host_port_section.grid(row=1, column=0, columnspan=2, sticky="ew", pady=20)

        ttk.Label(host_port_section, text="Host:", image=self.server_icon, compound="left", font=("Poppins", 14)).grid(
            row=0, column=0, padx=10, pady=10)
        self.host_entry = ttk.Entry(host_port_section, font=("Poppins", 12), width=20)
        self.host_entry.grid(row=0, column=1, padx=10, pady=10)
        self.host_entry.insert(0, "localhost")

        ttk.Label(host_port_section, text="SUB Port:", font=("Poppins", 14)).grid(row=0, column=2, padx=10, pady=10)
        self.sub_port_entry = ttk.Entry(host_port_section, font=("Poppins", 12), width=10)
        self.sub_port_entry.grid(row=0, column=3, padx=10, pady=10)
        self.sub_port_entry.insert(0, "5500")

        ttk.Label(host_port_section, text="REQ Port:", font=("Poppins", 14)).grid(row=0, column=4, padx=10, pady=10)
        self.req_port_entry = ttk.Entry(host_port_section, font=("Poppins", 12), width=10)
        self.req_port_entry.grid(row=0, column=5, padx=10, pady=10)
        self.req_port_entry.insert(0, "5600")

        # Listboxes
        self.queue_listbox = self.create_listbox(content_frame, "Students in Queue", row=2, column=0)
        self.supervisor_listbox = self.create_listbox(content_frame, "Available Supervisors", row=2, column=1)

        # Status Label
        self.status_label = ttk.Label(content_frame, text="", image='', compound="left",
                                      font=("Poppins", 12), anchor="e", foreground="#ffffff")

        self.status_label.grid(row=5, column=0, columnspan=2, sticky="ew")

        self.logic = QueueLogic(self)

    def create_listbox(self, parent, title, row, column, columnspan=1):
        ttk.Label(parent, text=title, font=("Poppins", 14, "bold"), foreground="#ffffff").grid(row=row, column=column,
                                                                                               columnspan=columnspan,
                                                                                               pady=10)
        listbox = Listbox(parent, height=15, width=40, bg="#333333", fg="#ffffff", selectbackground="#00b09b",
                          selectforeground="white", borderwidth=1, highlightthickness=0, font=("Poppins", 12))
        listbox.grid(row=row + 1, column=column, columnspan=columnspan, sticky="ew", padx=(20,0))
        return listbox

    def connect_to_server(self):
        self.logic.connect_to_server()

    def update_queue(self, queue_data):
        self.queue_listbox.delete(0, tk.END)
        for index, student in enumerate(queue_data, start=1):
            self.queue_listbox.insert(tk.END, f"{index}. {student['name']}")

    def update_supervisors(self, supervisors_data):
        self.supervisor_listbox.delete(0, tk.END)
        for supervisor in supervisors_data:
            name = supervisor.get('name', '')
            status = supervisor.get('status', '')
            client = supervisor.get('client', '')
            self.supervisor_listbox.insert(tk.END, f"{name} - {status} - {client}")

    def send_heartbeat(self):
        self.logic.send_heartbeat()
        self.after(3000, self.send_heartbeat)


if __name__ == "__main__":
    app = QueueUI()
    app.mainloop()
